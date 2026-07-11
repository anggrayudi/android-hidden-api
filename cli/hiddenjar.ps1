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

# Works on both Windows PowerShell 5.1 (no $IsWindows) and PowerShell 7+.
$OnWindows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT

# Options
$OptSerial = $null; $OptAvd = $null; $OptApi = $null; $OptSdkDir = $null
$OptOnlyFramework = $false; $OptOutput = $null; $OptInstall = $false
$OptDexTools = $null; $OptWorkDir = $null; $OptKeep = $false
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

    # Merge: SDK android.jar first (base), framework classes overlaid on top.
    Write-Log "Merging into custom android.jar ..."
    Push-Location $merged
    try {
        & $jarTool xf $baseJar
        Get-ChildItem -Path $classes -Filter '*-classes.jar' | ForEach-Object { & $jarTool xf $_.FullName }
    } finally { Pop-Location }
    $metaInf = Join-Path $merged 'META-INF'
    if (Test-Path $metaInf) { Remove-Item -Recurse -Force $metaInf }

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

    if (-not (Test-Compile $output $work $javac)) { Die "verification failed: hidden-API probe did not compile" }

    if ($OptInstall) { Install-Jar $output $platformDir $jarTool }

    if ($workIsTemp -and -not $OptKeep) { Remove-Item -Recurse -Force $work }
    elseif ($OptKeep -or $OptWorkDir) { Write-Note "Work dir kept at: $work" }
    Write-Log "Done."
}

function Test-Compile {
    param($jar, $work, $javac)
    $probe = Join-Path $work 'probe'
    New-Item -ItemType Directory -Path $probe -Force | Out-Null
    $src = Join-Path $probe 'HiddenApiProbe.java'
    @'
import android.app.ActivityThread;
import android.os.IBinder;
import android.os.ServiceManager;

// Uses long-standing @hide symbols to prove the custom jar exposes hidden APIs.
public class HiddenApiProbe {
    IBinder windowService() { return ServiceManager.getService("window"); }
    ActivityThread currentThread() { return ActivityThread.currentActivityThread(); }
}
'@ | Set-Content -Path $src -Encoding ASCII
    $log = Join-Path $probe 'javac.log'
    & $javac -nowarn -cp $jar -d (Join-Path $probe 'out') $src 2> $log
    if ($LASTEXITCODE -eq 0) {
        Write-Log "Verification OK: hidden-API probe compiled (ServiceManager.getService, ActivityThread.currentActivityThread)"
        return $true
    }
    Write-Warn "javac output:"; Get-Content $log -ErrorAction SilentlyContinue | Write-Host
    return $false
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
  --all-bootclasspath  merge every \$BOOTCLASSPATH jar that has DEX (default; full coverage)
  --output FILE        output path (default: .\android-<api>-custom.jar)
  --install            install into <sdk>\platforms\android-<api>*\android.jar (auto-backup .orig)
  --dex-tools DIR      path to an unpacked dex-tools distribution (default: auto-download + cache)
  --work-dir DIR       scratch dir (default: temp; kept on failure)
  --keep               keep the work dir even on success
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
