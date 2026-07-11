# Analysis: Building a Custom `android.jar` (Hidden APIs) via an Android Emulator + CLI Design

> These are the notes from a hands-on experiment run on one machine (macOS, Android SDK at
> `~/Library/Android/sdk`) on 2026-07-12. Every size and compilation result below is a **real
> observation**, not recalled from memory. Anything not actually tested is flagged explicitly as
> "assumption / not tested".

## TL;DR (verdict)

**Yes — a custom `android.jar` can be built entirely from an emulator (no physical device), and the
process is worth turning into a CLI.** It was proven end-to-end for API 37: `adb pull` `framework.jar`
(48 MB) from the emulator → `dex2jar` → merge into the SDK `android.jar` → **code using hidden/internal
APIs compiles successfully** (`ServiceManager.getService`, `ActivityThread.currentApplication`,
`com.android.internal.util.CharSequences`).

**Two findings change the shape of the solution:**

1. **The emulator's `framework.jar` size depends on the image version — it is not "always tiny".**
   The common experience (emulator = a few KB) holds for **older** images but not for newer ones.
   Measured on this machine:

   | Source | API | `/system/framework/framework.jar` size | Contents |
   |---|---|---|---|
   | Emulator `Small_Phone_API_24` (google_apis, arm64) | 24 | **318 bytes** | empty (only `MANIFEST.MF`) |
   | Emulator `Pixel_10_API_37` (google_apis, arm64) | 37 | **48,699,241 bytes (≈48 MB)** | 5 full DEX files |
   | Physical `SM-A525F` | 34 | **51,011,993 bytes (≈51 MB)** | full |

   So a high-API emulator (34/35/37) is **enough** to replace a physical device. The only problem is
   older images that strip `classes.dex` out of the jar (see §2).

2. **`framework.jar` alone is NOT enough on modern Android.** Many hidden APIs now live in other boot
   jars and **mainline modules (APEX)**. Verified example: the real implementation of
   `android.net.wifi.WifiManager` lives in `/apex/com.android.wifi/javalib/framework-wifi.jar`, **not**
   in `framework.jar`. The API 37 emulator's `BOOTCLASSPATH` has **52 jars**. A recipe that merges only
   `framework.jar` is outdated for API 30+.

---

## 1. The existing recipe (recap of the Medium article)

The "Krabby Patty recipe" from [Create Your Own Android Hidden APIs](https://hardiannicko.medium.com/create-your-own-android-hidden-apis-fa3cca02d345):

1. Pull `framework.jar` from a device: `adb pull /system/framework/framework.jar` (the article says it
   must be > 5 MB; a tiny file means the pull failed).
2. Rename to `.zip`, extract → get `classes.dex`, `classes2.dex`, etc.
3. Convert each DEX to `.class` with dex2jar / dex-tools (`d2j-dex2jar.sh`).
4. Extract the SDK `android.jar`, overlay the converted `.class` files, repack (`jar cvf`).
5. Copy to `<SDK>/platforms/android-XX/android.jar`, set `compileSdk`/`targetSdk`, rebuild.

The flow is correct in principle, but assumes (a) the source jar is "fat", and (b) all hidden APIs are
in `framework.jar`. Both assumptions break on modern Android.

---

## 2. Why can the emulator's `framework.jar` be only a few KB?

This is the crux of the "tiny jar" experience. The cause is not an emulator bug but a **pre-compilation
(AOT/odex) strategy in certain images**:

- On older images, framework classes are **pre-optimized** at build time into the **boot image**
  (`boot.art` / `boot*.oat` / `boot*.vdex`), and the `.jar` under `/system/framework/` has its
  `classes.dex` **stripped** (the deodexed jar becomes a shell holding only `MANIFEST.MF`). That is
  exactly what happened on the API 24 emulator here → **318 bytes**.
- On newer images (API 34/35/37), the image still has an AOT boot image
  (`/system/framework/oat/arm64/*.odex` + `*.vdex` — confirmed present on the API 37 emulator), **but**
  `framework.jar` still keeps the full DEX (48 MB). This is most likely because the image is a
  `userdebug` / `dev-keys` flavor (checked: `ro.build.flavor = sdk_gphone16k_arm64-userdebug`,
  `ro.build.tags = dev-keys`) that does not strip the jar.

**Practical implication for automation:** the CLI must **validate the size/contents of each jar** after
`adb pull`. If a jar contains only `MANIFEST.MF` (no `classes*.dex`), it is skipped, and:

- **Primary solution (recommended):** use an emulator with a **high-API image** that ships full DEX
  (API 37 proven OK). This removes the "tiny jar" problem entirely.
- **Fallback (only if stuck on an old image):** extract classes from the boot image
  (`boot-framework.vdex`/`.oat`) using a tool such as `vdexExtractor` followed by `baksmali`/`dex2jar`.
  **Not tested in this session** — flagged as an experimental path, not the happy path.

---

## 3. The experiment that was run (real evidence)

Environment: `Pixel_10_API_37` (emulator, arm64), `dex-tools 2.4.37`
([ThexXTURBOXx/dex2jar](https://github.com/ThexXTURBOXx/dex2jar)), JDK 17 (Zulu),
SDK `android.jar` for android-37.0 (43 MB).

```bash
# 1. Pull framework.jar from the emulator (NOT a physical device)
adb -s emulator-5554 pull /system/framework/framework.jar .
#    → 48,699,241 bytes, contains classes.dex..classes5.dex

# 2. Convert DEX → class
d2j-dex2jar.sh --force -o framework-classes.jar framework.jar
#    → 36,590 .class files, of which 6,295 are under com/android/internal/*

# 3. Merge: SDK android.jar as the base, overlaid by the framework classes
mkdir merged && cd merged
unzip -q <SDK>/platforms/android-37.0/android.jar   # base (public stubs + java.*, org.*, javax.*)
unzip -q -o ../framework-classes.jar                # overlay real impls (carry hidden APIs)
jar cf ../android-custom-37.jar -C . .
#    → android-custom-37.jar ≈ 65 MB
```

**Compilation check (the real success criterion — compileSdk/target 37 must compile hidden APIs):**

```java
import android.app.ActivityThread;
import android.os.ServiceManager;
import android.os.IBinder;

public class TestHidden {
    void test() {
        IBinder b = ServiceManager.getService("window");                 // hidden
        android.app.Application app = ActivityThread.currentApplication(); // hidden
        CharSequence s = com.android.internal.util.CharSequences
                .forAsciiBytes(new byte[0]);                             // com.android.internal
    }
}
```

```bash
javac -cp android-custom-37.jar TestHidden.java
# → COMPILE OK  (three hidden APIs used, without reflection)
```

`javap -cp android-custom-37.jar android.os.ServiceManager` also shows the full public method set
(`getService`, `addService`, `checkService`, …) that the stock `android.jar` does not have. **Proven.**

### Why merge order matters

The SDK `android.jar` is unzipped **first**, then overlaid by `framework-classes.jar`, because:

- `android.jar` holds many classes **not present** in `framework.jar`: `java.*`, `javax.*`,
  `org.w3c/xml/json`, and stubs from other boot jars. These must be preserved.
- For classes present in both (`android.*`, `com.android.internal.*`), the **framework** version wins,
  because that is the real implementation carrying the `@hide` members.

---

## 4. Key finding: `framework.jar` alone is incomplete (mainline/APEX)

This is what separates modern Android from the era of the Medium article. Verified on this machine:

- `android.net.wifi.WifiManager.class` **exists as a stub** in the SDK `android.jar`, but its real
  implementation is in `/apex/com.android.wifi/javalib/framework-wifi.jar` (which has `classes.dex`),
  **not** in `framework.jar`.
- The API 37 emulator's `BOOTCLASSPATH` = **52 entries**, spread across `/system/framework/*.jar`,
  `/apex/com.android.art/javalib/*`, `/apex/com.android.tethering/*`, `/apex/com.android.bt/*`, etc.

**Consequence:** for truly complete hidden-API coverage, the CLI must pull **every jar on
`$BOOTCLASSPATH`** (that contains DEX), convert them all, then overlay onto `android.jar`. Merging only
`framework.jar` leaves some hidden APIs (Wi-Fi, connectivity, Bluetooth, media, …) unresolved at
compile time.

---

## 5. Limitations worth stating honestly (risk)

1. **Compile-time ≠ runtime.** The custom jar only makes code **compile**. Since Android 9 (API 28)
   there is a **hidden API blocklist** that restricts non-SDK access **at runtime** for non-system apps
   → possible `NoSuchMethodError`/`ClassNotFoundException`. This is outside the custom jar's control.
   (Common Android knowledge; not re-tested this session.)
2. **Unofficial and version-fragile.** Hidden API signatures change without compatibility guarantees.
   The custom jar must be rebuilt per API level.
3. **dex2jar classes carry bodies, not just stubs.** For `javac` this is harmless (it only reads
   signatures), but the jar is larger (≈65 MB vs. 43 MB). Optional: a "stub-only" pass
   (e.g. `d2j-jar2jasmin` / ASM strip) to slim it down — **not tested**.
4. **The old-image fallback (vdex extraction) is untested** this session (§2).
5. **ABI:** an arm64 emulator on Apple Silicon is fine here; DEX→class conversion is ABI-independent, so
   it is not an issue.

---

## 6. Ready-made alternatives (for comparison)

If the goal is simply "have a jar", prebuilt options exist:
[Reginer/aosp-android-jar](https://github.com/Reginer/aosp-android-jar) and
[JetpackDuba/android-jar-with-hidden-api](https://github.com/JetpackDuba/android-jar-with-hidden-api).
This CLI is still useful for (a) API levels not yet published as prebuilts, (b) custom/OEM images, and
(c) reproducible internal builds without downloading third-party binaries.

---

## 7. CLI design

### 7.1 Name & form

`hiddenjar` — a single **Bash script** (the most portable option, mirrors the manual steps, needs no
build). A thin **Gradle wrapper** on top (`./gradlew buildHiddenJar -Papi=37`) is convenient for
contributors, but the Bash script remains the source of truth.

### 7.2 Pipeline

```
[detect adb/JDK] → [pick/boot emulator] → [read API level]
      → [read $BOOTCLASSPATH] → [adb pull each jar]
      → [validate jar contents: skip those without classes.dex]
      → [dex2jar each jar that has DEX]
      → [locate SDK android.jar for the API level]
      → [merge: unzip android.jar → overlay all framework classes → repack]
      → [VERIFY: compile a hidden-API probe .java]  ← mandatory success gate
      → [optional --install: backup + copy into <SDK>/platforms/android-XX/android.jar]
```

### 7.3 Interface (as shipped)

```
hiddenjar build \
  [--api 37] \                   # target API level (default: read from device)
  [--serial emulator-5554] \     # device/emulator; default: auto-pick a running emulator
  [--avd Pixel_10_API_37] \      # if nothing is running, boot this AVD headless first
  [--sdk-dir ~/Library/Android/sdk] \
  [--only-framework] \           # merge framework.jar only (old mode: faster, less complete)
  [--all-bootclasspath] \        # DEFAULT: merge every $BOOTCLASSPATH jar (complete)
  [--output ./android-37-custom.jar] \
  [--install] \                  # install into <SDK>/platforms/android-37*/ (auto-backup .orig)
  [--dex-tools <path>]           # default: auto-download dex-tools to the cache
```

Supporting commands: `hiddenjar doctor` (check adb/JDK/dex-tools), `hiddenjar restore --api 37`
(restore `android.jar.orig`). A signature-only (`--stub-only`) mode and auto-downloading a missing SDK
`android.jar` from `dl.google.com` are noted as **future ideas, not implemented in v1**.

### 7.4 Critical implementation points (from findings §2–§4)

- **Jar validation is mandatory:** after pull, check `unzip -l | grep classes.*dex`. If empty → the jar
  was stripped; log and continue. If **all** jars are empty (old image), the CLI stops with a clear
  message: "use an API ≥ 34 image or a physical device" (the vdex fallback is optional & experimental).
- **Merge the whole `$BOOTCLASSPATH`, not just `framework.jar`** (default), so mainline/APEX APIs
  (Wi-Fi, Bluetooth, connectivity, media) are included.
- **Overlay order:** SDK `android.jar` first (base) → framework classes overlaid. Preserve public SDK
  classes.
- **Idempotent & safe:** `--install` always backs up `android.jar` → `android.jar.orig` first.
- **Success gate = probe compilation.** The CLI does not report success just because the repack
  finished; it compiles a hidden-API probe first. If that fails → non-zero exit.
- **Cache dex-tools** in `~/.cache/hiddenjar/` to avoid re-downloading.

### 7.5 Prerequisites

`adb` (platform-tools), a JDK (`javac`/`jar`), `dex-tools` (auto-downloaded), `unzip`/`zip`, and at
least one **emulator at API ≥ 34** or a device.

---

## 8. Conclusion

- **An emulator can replace a physical device** for building a custom `android.jar`, **as long as the
  image ships a `framework.jar` that contains DEX** (proven: API 37 = 48 MB; API 24 = 318 bytes cannot).
  This both explains and corrects the "the emulator's framework.jar is always tiny" experience: that is
  specific to older images.
- **Hidden-API compilation is proven to work** with the custom jar for compile/target SDK 37 — exactly
  the requested test criterion.
- **CLI automation is very feasible**, with two mandatory improvements over the old recipe: (1) validate
  jar contents to handle stripped jars, and (2) merge the whole `$BOOTCLASSPATH` (not just
  `framework.jar`) for mainline/APEX coverage.

### Sources
- [Create Your Own Android Hidden APIs — Medium (hardiannicko)](https://hardiannicko.medium.com/create-your-own-android-hidden-apis-fa3cca02d345)
- [ThexXTURBOXx/dex2jar](https://github.com/ThexXTURBOXx/dex2jar)
- [Reginer/aosp-android-jar](https://github.com/Reginer/aosp-android-jar)
- [JetpackDuba/android-jar-with-hidden-api](https://github.com/JetpackDuba/android-jar-with-hidden-api)
