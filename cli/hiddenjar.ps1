#!/usr/bin/env pwsh
#
# hiddenjar.ps1 — native Windows/PowerShell port of cli/hiddenjar.
#
# Builds a custom android.jar (with hidden/internal APIs) from an emulator/device.
# Cross-platform: Windows PowerShell 5.1+ and PowerShell 7+ (also runs on macOS/Linux).
# Uses `jar` (from the JDK, already required) for all zip work, so there is NO
# dependency on `unzip` — that is what makes it work natively on Windows.
#
# Same command/flag surface as the bash script:
#   powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 build --api 37
#   powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 doctor
#   powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 restore --api 37
#
# See docs/BUILD_CUSTOM_ANDROID_JAR_FROM_EMULATOR.md

$ErrorActionPreference = 'Stop'

# ----------------------------------------------------------------------------
# Constants & globals
# ----------------------------------------------------------------------------
$Version         = '1.0.0'
$DexToolsVersion = '2.4.37'
$DexToolsUrl     = "https://github.com/ThexXTURBOXx/dex2jar/releases/download/$DexToolsVersion/dex-tools-$DexToolsVersion.zip"
$CacheDir        = Join-Path $HOME '.cache/hiddenjar'
$MinJarBytes     = 1024

# Directory of this script — used to locate the bundled Stubifier.java.
$ScriptDir    = $PSScriptRoot
$StubifierSrc = Join-Path $ScriptDir 'Stubifier.java'
$ClosureSrc   = Join-Path $ScriptDir 'ClosureVerify.java'

# Works on both Windows PowerShell 5.1 (no $IsWindows) and PowerShell 7+.
$OnWindows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT

# Options
$OptSerial = $null; $OptAvd = $null; $OptApi = $null; $OptSdkDir = $null
$OptOnlyFramework = $false; $OptOutput = $null; $OptInstall = $false
$OptDexTools = $null; $OptWorkDir = $null; $OptKeep = $false; $OptKeepBodies = $false
$D2J = $null; $Adb = $null

# ----------------------------------------------------------------------------
# Logging
# ----------------------------------------------------------------------------
function Write-Log  { param($m) Write-Host "[hiddenjar] $m" -ForegroundColor Green }
function Write-Note { param($m) Write-Host "[hiddenjar] $m" -ForegroundColor DarkGray }
function Write-Warn { param($m) Write-Host "[hiddenjar] WARN: $m" -ForegroundColor Yellow }
function Die        { param($m) Write-Host "[hiddenjar] ERROR: $m" -ForegroundColor Red; exit 1 }

function Exe { param($base) if ($OnWindows) { "$base.exe" } else { $base } }

# ----------------------------------------------------------------------------
# Environment resolution
# ----------------------------------------------------------------------------
function Resolve-SdkDir {
    if ($OptSdkDir)              { return $OptSdkDir }
    if ($env:ANDROID_HOME)       { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT)   { return $env:ANDROID_SDK_ROOT }
    if ($OnWindows)              { return (Join-Path $env:LOCALAPPDATA 'Android\Sdk') }
    $macPath = Join-Path $HOME 'Library/Android/sdk'
    if (Test-Path $macPath)      { return $macPath }
    return (Join-Path $HOME 'Android/Sdk')
}

function Resolve-Adb {
    $sdk  = Resolve-SdkDir
    $cand = Join-Path $sdk (Join-Path 'platform-tools' (Exe 'adb'))
    if (Test-Path $cand) { return $cand }
    $cmd = Get-Command (Exe 'adb') -ErrorAction SilentlyContinue
    if (-not $cmd) { $cmd = Get-Command 'adb' -ErrorAction SilentlyContinue }
    if ($cmd) { return $cmd.Source }
    return $null
}

function Resolve-Tool {
    param($base)
    $c = Get-Command (Exe $base) -ErrorAction SilentlyContinue
    if (-not $c) { $c = Get-Command $base -ErrorAction SilentlyContinue }
    if ($c) { return $c.Source }
    return $null
}

function Resolve-PlatformDir {
    param($sdk, $api)
    foreach ($d in @((Join-Path $sdk "platforms/android-$api"), (Join-Path $sdk "platforms/android-$api.0"))) {
        if (Test-Path (Join-Path $d 'android.jar')) { return $d }
    }
    $cands = Get-ChildItem -Path (Join-Path $sdk 'platforms') -Directory -Filter "android-$api*" -ErrorAction SilentlyContinue
    foreach ($c in $cands) {
        $aj = Join-Path $c.FullName 'android.jar'
        $sp = Join-Path $c.FullName 'source.properties'
        if ((Test-Path $aj) -and (Test-Path $sp) -and (Select-String -Path $sp -Pattern "AndroidVersion.ApiLevel=$api$" -Quiet)) {
            return $c.FullName
        }
    }
    return $null
}

function Find-DexTools {
    # dex-tools uses .bat on Windows, .sh elsewhere.
    $name = if ($OnWindows) { 'd2j-dex2jar.bat' } else { 'd2j-dex2jar.sh' }
    $roots = @()
    if ($OptDexTools) { $roots += $OptDexTools } else { $roots += $CacheDir }
    foreach ($r in $roots) {
        if (Test-Path $r) {
            $found = Get-ChildItem -Path $r -Recurse -Filter $name -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) { return $found.FullName }
        }
    }
    return $null
}

function Ensure-DexTools {
    $name = if ($OnWindows) { 'd2j-dex2jar.bat' } else { 'd2j-dex2jar.sh' }
    $found = Find-DexTools
    if (-not $found) {
        if ($OptDexTools) { Die "--dex-tools '$OptDexTools' has no $name" }
        if (-not (Get-Command 'curl' -ErrorAction SilentlyContinue) -and -not (Get-Command 'Invoke-WebRequest' -ErrorAction SilentlyContinue)) {
            Die "dex-tools not cached and no downloader available; pass --dex-tools DIR"
        }
        if (-not (Test-Path $CacheDir)) { New-Item -ItemType Directory -Path $CacheDir -Force | Out-Null }
        Write-Log "Downloading dex-tools $DexToolsVersion ..."
        $zip = Join-Path $CacheDir 'dex-tools.zip'
        Invoke-WebRequest -Uri $DexToolsUrl -OutFile $zip
        Expand-Archive -Path $zip -DestinationPath $CacheDir -Force
        $found = Find-DexTools
        if (-not $found) { Die "dex-tools download did not contain $name" }
    }
    $script:D2J = $found
    if (-not $OnWindows) {
        Get-ChildItem -Path (Split-Path $script:D2J) -Filter '*.sh' -ErrorAction SilentlyContinue |
            ForEach-Object { & chmod +x $_.FullName }
    }
}

# ----------------------------------------------------------------------------
# Device selection
# ----------------------------------------------------------------------------
function Get-Devices {
    $out = & $script:Adb devices
    $out | Select-Object -Skip 1 | ForEach-Object {
        $parts = ($_ -split '\s+')
        if ($parts.Count -ge 2 -and $parts[1] -eq 'device') { $parts[0] }
    }
}

function Boot-Avd {
    if (-not $OptAvd) { return }
    $sdk = Resolve-SdkDir
    $emu = Join-Path $sdk (Join-Path 'emulator' (Exe 'emulator'))
    if (-not (Test-Path $emu)) { Die "emulator binary not found at $emu" }
    Write-Log "Booting AVD '$OptAvd' headless ..."
    Start-Process -FilePath $emu -ArgumentList @('-avd', $OptAvd, '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot') -WindowStyle Hidden | Out-Null
    $tries = 0
    while ((@(Get-Devices).Count -eq 0) -and ($tries -lt 60)) { Start-Sleep -Seconds 2; $tries++ }
    $emuDev = @(Get-Devices) | Where-Object { $_ -like 'emulator-*' } | Select-Object -First 1
    if (-not $emuDev) { $emuDev = @(Get-Devices) | Select-Object -First 1 }
    if (-not $emuDev) { Die "AVD did not come online" }
    $script:OptSerial = $emuDev
    & $script:Adb -s $script:OptSerial wait-for-device
    Write-Log "Waiting for boot to complete ..."
    $tries = 0
    while ((("$(& $script:Adb -s $script:OptSerial shell getprop sys.boot_completed)").Trim() -ne '1') -and ($tries -lt 90)) {
        Start-Sleep -Seconds 2; $tries++
    }
}

function Pick-Serial {
    if ($OptSerial) { return }
    Boot-Avd
    if ($OptSerial) { return }
    $devs = @(Get-Devices)
    $emu  = $devs | Where-Object { $_ -like 'emulator-*' } | Select-Object -First 1
    if ($emu)             { $script:OptSerial = $emu }
    elseif ($devs.Count)  { $script:OptSerial = $devs[0] }
    else                  { Die "no adb device online — start an emulator or pass --avd NAME" }
}

# ----------------------------------------------------------------------------
# build
# ----------------------------------------------------------------------------
function Invoke-Build {
    $script:Adb = Resolve-Adb
    if (-not $script:Adb) { Die "adb not found (looked in $(Resolve-SdkDir)/platform-tools and PATH)" }
    Ensure-DexTools
    $javac = Resolve-Tool 'javac'; if (-not $javac) { Die "javac not found (install a JDK)" }
    $jarTool = Resolve-Tool 'jar'; if (-not $jarTool) { Die "jar not found (install a JDK)" }

    Pick-Serial
    Write-Log "Target device: $($script:OptSerial)"

    $api = $OptApi
    if (-not $api) { $api = ("$(& $script:Adb -s $script:OptSerial shell getprop ro.build.version.sdk)").Trim() }
    if (-not $api) { Die "could not read API level from device" }
    Write-Log "API level: $api"

    $sdk = Resolve-SdkDir
    $platformDir = Resolve-PlatformDir $sdk $api
    if (-not $platformDir) {
        Write-Warn "No SDK platform for API $api. Installed platforms:"
        Get-ChildItem -Path (Join-Path $sdk 'platforms') -Directory -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "  $($_.Name)" }
        Die "install it via sdkmanager `"platforms;android-$api`" or pass --sdk-dir"
    }
    $baseJar = Join-Path $platformDir 'android.jar'
    Write-Log "SDK base android.jar: $baseJar"

    # Work dir
    if ($OptWorkDir) { $work = $OptWorkDir; New-Item -ItemType Directory -Path $work -Force | Out-Null; $workIsTemp = $false }
    else {
        $work = Join-Path ([IO.Path]::GetTempPath()) ("hiddenjar-" + [Guid]::NewGuid().ToString('N').Substring(0,8))
        New-Item -ItemType Directory -Path $work -Force | Out-Null; $workIsTemp = $true
    }
    $pulled  = Join-Path $work 'pulled'
    $classes = Join-Path $work 'classes'
    $merged  = Join-Path $work 'merged'
    New-Item -ItemType Directory -Path $pulled, $classes, $merged -Force | Out-Null

    # Jars to pull
    if ($OptOnlyFramework) {
        $jarList = @('/system/framework/framework.jar')
    } else {
        $bcp = "$(& $script:Adb -s $script:OptSerial shell 'echo $BOOTCLASSPATH')"
        $jarList = ($bcp -replace "`r", '') -split ':' | Where-Object { $_ -ne '' }
    }
    $total = @($jarList).Count
    Write-Log "Boot classpath entries to consider: $total"

    $converted = 0; $skipped = 0; $failed = 0; $idx = 0
    foreach ($devPath in $jarList) {
        $idx++
        $localName = ($devPath -replace '^/', '') -replace '/', '_'
        $localJar  = Join-Path $pulled $localName
        & $script:Adb -s $script:OptSerial pull $devPath $localJar 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $localJar)) { Write-Warn "[$idx/$total] pull failed: $devPath"; $failed++; continue }
        $size = (Get-Item $localJar).Length
        $dexCount = @(& $jarTool tf $localJar 2>$null | Select-String -Pattern 'classes\d*\.dex$').Count
        if ($size -lt $MinJarBytes -or $dexCount -eq 0) { Write-Note "[$idx/$total] skip (stripped/no DEX, ${size}B): $devPath"; $skipped++; continue }
        $outJar = Join-Path $classes (($localName -replace '\.jar$', '') + '-classes.jar')
        & $script:D2J --force -o $outJar $localJar 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0 -and (Test-Path $outJar)) { Write-Note "[$idx/$total] converted ($dexCount dex): $devPath"; $converted++ }
        else { Write-Warn "[$idx/$total] dex2jar failed: $devPath"; $failed++ }
    }
    Write-Log "Jars converted: $converted, skipped: $skipped, failed: $failed"
    if ($converted -eq 0) { Die "no framework jars had usable DEX — use an API >= 34 image or a device" }

    # Merge: SDK android.jar first (base = the curated public API surface), then overlay the
    # device's real framework bytecode so @hide members and com.android.internal appear.
    #
    # Filter the overlay by NAMESPACE, not by source jar. The boot classpath also carries the full
    # ART runtime (java/*, sun/*, jdk/*, javax/*, libcore/*) and repackaged libraries
    # (com.android.okhttp, com.android.org.bouncycastle, org.apache.xml*, gov.nist, ...). None of
    # those belong on the compile classpath; overlaying them shadows the JDK and the app's own
    # dependencies, so Kotlin/Gradle then reject the jar ("Cannot access <X> which is a supertype
    # ... missing or conflicting dependencies", issue #100). Keep only what the SDK is meant to
    # expose: android.* (public + @hide, incl. APEX modules like framework-wifi), com.android.internal.*
    # and dalvik.* (e.g. @hide dalvik.system.VMRuntime). The base jar already provides the curated
    # java.*/javax.*/org.*/dalvik.* public stubs, so we never let the overlay clobber those.
    #
    # The JDK 'jar' tool can't extract by wildcard the way unzip does, so extract each overlay jar
    # to a scratch dir and copy only the allowlisted namespace subtrees into $merged.
    Write-Log "Merging into custom android.jar (overlay filtered to hidden-API namespaces) ..."
    Push-Location $merged
    try { & $jarTool xf $baseJar } finally { Pop-Location }
    $overlayNs = @('android', 'com/android/internal', 'dalvik')  # forward slashes: portable across Win/*nix
    Get-ChildItem -Path $classes -Filter '*-classes.jar' | ForEach-Object {
        $ovl = Join-Path $work ('ovl-' + [IO.Path]::GetFileNameWithoutExtension($_.Name))
        if (Test-Path $ovl) { Remove-Item -Recurse -Force $ovl }
        New-Item -ItemType Directory -Path $ovl -Force | Out-Null
        Push-Location $ovl
        try { & $jarTool xf $_.FullName } finally { Pop-Location }
        foreach ($ns in $overlayNs) {
            $srcRoot = Join-Path $ovl $ns
            if (-not (Test-Path $srcRoot)) { continue }
            Get-ChildItem -Path $srcRoot -Recurse -File | ForEach-Object {
                $rel = $_.FullName.Substring($ovl.Length).TrimStart('\', '/')
                $dst = Join-Path $merged $rel
                $dstDir = Split-Path $dst -Parent
                if (-not (Test-Path $dstDir)) { New-Item -ItemType Directory -Path $dstDir -Force | Out-Null }
                Copy-Item -LiteralPath $_.FullName -Destination $dst -Force
            }
        }
        Remove-Item -Recurse -Force $ovl
    }
    $metaInf = Join-Path $merged 'META-INF'
    if (Test-Path $metaInf) { Remove-Item -Recurse -Force $metaInf }

    # Strip real method bodies to stubs (default) so Gradle's MockableJarTransform accepts the jar.
    if ($OptKeepBodies) {
        Write-Warn "--keep-bodies: keeping real method bodies; Gradle lint/unit tests (MockableJarTransform) will FAIL on this jar"
    } else {
        Invoke-Stubify $merged $work $javac
    }

    $output = $OptOutput
    if (-not $output) { $output = Join-Path (Get-Location).Path "android-$api-custom.jar" }
    $output = [IO.Path]::GetFullPath($output)
    & $jarTool cf $output -C $merged .
    if ($LASTEXITCODE -ne 0) { Die "jar packaging failed" }

    $outsize    = (Get-Item $output).Length
    $entries    = & $jarTool tf $output
    $allclasses = @($entries | Select-String -Pattern '\.class$').Count
    $internal   = @($entries | Select-String -Pattern 'com/android/internal/').Count
    Write-Log "Built: $output ($outsize bytes, $allclasses classes, $internal com.android.internal)"

    if (-not (Test-Compile $output $baseJar $work $javac $jarTool)) { Die "verification failed (see checks above)" }

    if ($OptInstall) { Install-Jar $output $platformDir $jarTool }

    if ($workIsTemp -and -not $OptKeep) { Remove-Item -Recurse -Force $work }
    elseif ($OptKeep -or $OptWorkDir) { Write-Note "Work dir kept at: $work" }
    Write-Log "Done."
}

# Strip every .class under a directory to a signature-only stub, in place (see cli/Stubifier.java).
#
# hiddenjar overlays real dex2jar-decompiled framework bodies onto android.jar. Those bodies carry
# try/catch blocks, and Gradle's MockableJarGenerator (lint + unit tests) crashes on them
# (NPE "Cannot read field outgoingEdges ... handlerRangeBlock is null", issue #46). Reducing the
# classes to stubs — the shape the SDK's own android.jar ships — makes the transform pass; javac
# only reads signatures, so compilation of hidden/internal APIs is unaffected.
function Invoke-Stubify {
    param($dir, $work, $javac)
    $lib  = Join-Path (Split-Path $script:D2J) 'lib'
    $jars = @(Get-ChildItem -Path $lib -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    if ($jars.Count -eq 0) { Die "dex-tools lib (ASM) not found under $lib; cannot strip method bodies (pass --keep-bodies to skip)" }
    if (-not (Test-Path $StubifierSrc)) { Die "Stubifier.java not found at $StubifierSrc — keep the cli/ directory intact, or pass --keep-bodies" }
    $java = Resolve-Tool 'java'; if (-not $java) { Die "java not found (install a JDK)" }
    $sep = [IO.Path]::PathSeparator
    $cp  = ($jars -join $sep)
    $stubout = Join-Path $work 'stubifier-out'
    New-Item -ItemType Directory -Path $stubout -Force | Out-Null
    Write-Log "Stripping method bodies to signature-only stubs (fixes Gradle MockableJarTransform) ..."
    & $javac -cp $cp -d $stubout $StubifierSrc
    if ($LASTEXITCODE -ne 0) { Die "failed to compile Stubifier.java" }
    & $java -cp "$stubout$sep$cp" Stubifier $dir
    if ($LASTEXITCODE -ne 0) { Die "stubify pass failed" }
}

# The success gate — four independent checks, each catching a failure mode the others miss (see the
# bash script's verify_compile for the rationale; issue #100 slipped past the old single-probe gate).
function Test-Compile {
    param($jar, $baseJar, $work, $javac, $jarTool)
    $probe = Join-Path $work 'probe'
    New-Item -ItemType Directory -Path $probe -Force | Out-Null
    $ok = $true

    # (1) Compile probe: normal public API + supertype chains (View -> Drawable.Callback, Activity, ...)
    #     AND @hide symbols across android.*, dalvik.* and com.android.internal.*.
    $src = Join-Path $probe 'HiddenApiProbe.java'
    @'
import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.view.View;
import android.view.ViewGroup;
import dalvik.system.VMRuntime;

public abstract class HiddenApiProbe extends ViewGroup {   // walks View -> Drawable.Callback, KeyEvent.Callback, ...
    public HiddenApiProbe(Context c) { super(c); }
    Drawable.Callback asDrawableCallback() { return this; } // View implements Drawable.Callback (issue #100 supertype)
    Activity activity;
    Bundle bundle;
    View view;
    IBinder windowService() { return ServiceManager.getService("window"); } // @hide android.os
    ActivityThread currentThread() { return ActivityThread.currentActivityThread(); } // @hide android.app
    Object vmRuntime() { return VMRuntime.getRuntime(); } // @hide dalvik.system (absent from stock)
    Class<?> internal() { return com.android.internal.R.class; } // com.android.internal
}
'@ | Set-Content -Path $src -Encoding ASCII
    $log = Join-Path $probe 'javac.log'
    & $javac -nowarn -cp $jar -d (Join-Path $probe 'out') $src 2> $log
    if ($LASTEXITCODE -eq 0) {
        Write-Note "verify(1/4) compile probe OK — View/ViewGroup/Drawable.Callback/Activity + @hide resolve"
    } else {
        Write-Warn "verify(1/4) compile probe FAILED — the jar cannot resolve core public + hidden APIs:"
        Get-Content $log -ErrorAction SilentlyContinue | Write-Host
        $ok = $false
    }

    $entries = & $jarTool tf $jar

    # (2) Conflict guard: the jar must NOT ship the ART runtime / repackaged libs that shadow the JDK
    #     and the app's own dependencies (the issue-#100 cause; also guards the overlay filter).
    $polluted = @($entries |
        Where-Object { $_ -match '^(sun|jdk|libcore|com/google|com/android/okhttp|com/android/org|gov/nist|org/apache/(xml|xpath|xalan))/' } |
        ForEach-Object { ($_ -split '/')[0..1] -join '/' } | Sort-Object -Unique)
    if ($polluted.Count -gt 0) {
        Write-Warn ("verify(2/4) conflict guard FAILED — jar ships JDK/dependency-shadowing namespaces: " + ($polluted -join ' '))
        $ok = $false
    } else {
        Write-Note "verify(2/4) conflict guard OK — no JDK/dependency-shadowing namespaces"
    }

    # (3) Public-API floor: the merge overlays onto the SDK jar, so the result must never have FEWER
    #     classes than the base SDK jar.
    $outN  = @($entries | Select-String -Pattern '\.class$').Count
    $baseN = @((& $jarTool tf $baseJar) | Select-String -Pattern '\.class$').Count
    if ($outN -lt $baseN) {
        Write-Warn "verify(3/4) public-API floor FAILED — jar has $outN classes but base SDK jar has $baseN"
        $ok = $false
    } else {
        Write-Note "verify(3/4) public-API floor OK — $outN classes (>= $baseN in base SDK jar)"
    }

    # (4) Supertype closure: no android.*/dalvik.*/java.* class may be left with a dangling supertype
    #     (the exact issue-#100 symptom, e.g. View losing Drawable.Callback).
    if (Test-Closure $jar $work $javac) {
        Write-Note "verify(4/4) supertype closure OK"
    } else {
        Write-Warn "verify(4/4) supertype closure FAILED — a public supertype is missing (issue #100 symptom)"
        $ok = $false
    }

    if ($ok) { Write-Log "Verification OK: all 4 checks passed" } else { Write-Warn "Verification FAILED (see checks above)" }
    return $ok
}

# Compiles + runs the bundled ClosureVerify against the jar. Returns $false only on a HARD failure
# (a missing public supertype); a missing tool/ASM is reported and treated as a skip (returns $true)
# so the gate never blocks a build just because the checker itself could not run.
function Test-Closure {
    param($jar, $work, $javac)
    $lib  = Join-Path (Split-Path $script:D2J) 'lib'
    $jars = @(Get-ChildItem -Path $lib -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    if ($jars.Count -eq 0 -or -not (Test-Path $ClosureSrc)) {
        Write-Warn "closure check skipped (ASM lib or ClosureVerify.java not found)"; return $true
    }
    $java = Resolve-Tool 'java'; if (-not $java) { Write-Warn "closure check skipped (java not found)"; return $true }
    $sep = [IO.Path]::PathSeparator
    $cp  = ($jars -join $sep)
    $out = Join-Path $work 'closure-out'
    New-Item -ItemType Directory -Path $out -Force | Out-Null
    & $javac -cp $cp -d $out $ClosureSrc 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Warn "closure check skipped (could not compile ClosureVerify.java)"; return $true }
    & $java -cp "$out$sep$cp" ClosureVerify $jar
    return ($LASTEXITCODE -eq 0)
}

function Install-Jar {
    param($built, $platformDir, $jarTool)
    $target = Join-Path $platformDir 'android.jar'
    $backup = Join-Path $platformDir 'android.jar.orig'
    if (-not (Test-Path $backup)) { Copy-Item $target $backup; Write-Log "Backed up original SDK jar -> $backup" }
    else { Write-Note "Backup already exists (kept): $backup" }
    Copy-Item $built $target -Force
    Write-Log "Installed custom jar -> $target"
    Write-Log "Restore anytime with: hiddenjar.ps1 restore --api <level>"
}

# ----------------------------------------------------------------------------
# restore
# ----------------------------------------------------------------------------
function Invoke-Restore {
    $sdk = Resolve-SdkDir
    if (-not $OptApi) { Die "restore requires --api N" }
    $platformDir = Resolve-PlatformDir $sdk $OptApi
    if (-not $platformDir) { Die "no SDK platform for API $OptApi" }
    $target = Join-Path $platformDir 'android.jar'
    $backup = Join-Path $platformDir 'android.jar.orig'
    if (-not (Test-Path $backup)) { Die "no backup found at $backup" }
    Copy-Item $backup $target -Force
    Write-Log "Restored original SDK android.jar for API $OptApi"
}

# ----------------------------------------------------------------------------
# doctor
# ----------------------------------------------------------------------------
function Invoke-Doctor {
    $ok = 0
    $adb = Resolve-Adb
    if ($adb) { Write-Host "  adb:      $adb" } else { Write-Host "  adb:      MISSING"; $ok = 1 }
    $javac = Resolve-Tool 'javac'; if ($javac) { Write-Host "  javac:    $javac" } else { Write-Host "  javac:    MISSING"; $ok = 1 }
    $jarTool = Resolve-Tool 'jar'; if ($jarTool) { Write-Host "  jar:      $jarTool" } else { Write-Host "  jar:      MISSING"; $ok = 1 }
    $d2j = Find-DexTools
    if ($d2j) { Write-Host "  dex2jar:  $d2j" } else { Write-Host "  dex2jar:  not cached (auto-downloaded on build)" }
    Write-Host "  unzip:    not required (uses the JDK 'jar' tool)"
    Write-Host "  sdk-dir:  $(Resolve-SdkDir)"
    Write-Host "  os:       $([Environment]::OSVersion.Platform) (windows=$OnWindows)"
    if ($adb) {
        Write-Host "  devices:"
        & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S' } | ForEach-Object { Write-Host "    $_" }
    }
    if ($ok -eq 0) { Write-Log "doctor: all required tools present" } else { Write-Warn "doctor: some tools missing (see above)" }
    exit $ok
}

# ----------------------------------------------------------------------------
# Usage
# ----------------------------------------------------------------------------
function Show-Usage {
@"
hiddenjar.ps1 v$Version — build a custom android.jar with hidden APIs (native PowerShell)

USAGE:
  powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 build   [options]
  powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 restore --api N [--sdk-dir DIR]
  powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 doctor

BUILD OPTIONS (same flags as the bash script):
  --serial ID          adb device serial (default: auto — a running emulator, else first device)
  --avd NAME           if no device is running, boot this AVD headless first
  --api N              API level override (default: read from device)
  --sdk-dir DIR        Android SDK root (default: %LOCALAPPDATA%\Android\Sdk / ANDROID_HOME)
  --only-framework     merge only /system/framework/framework.jar (fast, incomplete)
  --all-bootclasspath  merge hidden-API namespaces (android.*, com.android.internal.*, dalvik.*)
                       from every \$BOOTCLASSPATH jar that has DEX (default; full coverage)
  --output FILE        output path (default: .\android-<api>-custom.jar)
  --install            install into <sdk>\platforms\android-<api>*\android.jar (auto-backup .orig)
  --dex-tools DIR      path to an unpacked dex-tools distribution (default: auto-download + cache)
  --work-dir DIR       scratch dir (default: temp; kept on failure)
  --keep               keep the work dir even on success
  --keep-bodies        keep real dex2jar method bodies instead of stripping them to signature-only
                       stubs. Bodies let you browse decompiled sources, but Gradle lint / unit tests
                       (MockableJarTransform) then FAIL on the jar. Default: strip to stubs.
"@ | Write-Host
}

# ----------------------------------------------------------------------------
# Argument parsing (mirrors the bash CLI: same --flags)
# ----------------------------------------------------------------------------
$Command = 'help'
$positional = @()
$argv = @($args)
$i = 0
while ($i -lt $argv.Count) {
    $a = [string]$argv[$i]
    switch ($a) {
        '--serial'           { $OptSerial = $argv[$i+1]; $i += 2; continue }
        '--avd'              { $OptAvd = $argv[$i+1]; $i += 2; continue }
        '--api'              { $OptApi = $argv[$i+1]; $i += 2; continue }
        '--sdk-dir'          { $OptSdkDir = $argv[$i+1]; $i += 2; continue }
        '--output'           { $OptOutput = $argv[$i+1]; $i += 2; continue }
        '--dex-tools'        { $OptDexTools = $argv[$i+1]; $i += 2; continue }
        '--work-dir'         { $OptWorkDir = $argv[$i+1]; $i += 2; continue }
        '--only-framework'   { $OptOnlyFramework = $true; $i += 1; continue }
        '--all-bootclasspath'{ $OptOnlyFramework = $false; $i += 1; continue }
        '--install'          { $OptInstall = $true; $i += 1; continue }
        '--keep'             { $OptKeep = $true; $i += 1; continue }
        '--keep-bodies'      { $OptKeepBodies = $true; $i += 1; continue }
        '-h'                 { $Command = 'help'; $i += 1; continue }
        '--help'             { $Command = 'help'; $i += 1; continue }
        default {
            if ($a -like '-*') { Die "unknown option: $a" }
            $positional += $a; $i += 1
        }
    }
}
if ($positional.Count -gt 0) { $Command = $positional[0] }

switch ($Command) {
    'build'   { Invoke-Build }
    'restore' { Invoke-Restore }
    'doctor'  { Invoke-Doctor }
    'help'    { Show-Usage }
    default   { Show-Usage; Die "unknown command: $Command" }
}
