# Android Hidden APIs

**Android Hidden APIs** are classes, methods and resources that Google hides from you because of stability reason.
These features are hidden because they may be changed on next API version.

The internal APIs are located in package `com.android.internal` and available in the `framework.jar`,
while the hidden APIs are located in the `android.jar` file with `@hide` javadoc attribute.
Now you know the difference. But I will refer to both as hidden APIs.

This repo contains custom `android.jar` which you can use to develop your app.
However, if you urgently need to create your own `android.jar`, I also share you the Krabby Patty
secret recipe here: [Create Your Own Android Hidden APIs](https://medium.com/@hardiannicko/create-your-own-android-hidden-apis-fa3cca02d345).

## Use Custom `android.jar`

1. ~~Download custom `android.jar` from [Google Drive](https://drive.google.com/drive/folders/17oMwQ0xBcSGn159mgbqxcXXEcneUmnph).~~ **We no longer upload jars to Google Drive** — every developer can now generate one for any API level with the [`hiddenjar` CLI](#build-your-own-custom-androidjar-cli). Build it, then continue from step 2.
2. Go to `<SDK location>/platforms/`.
3. Copy, paste and replace the downloaded hidden API file into this directory, e.g. `android-30/android.jar`.
4. Change `compileSdkVersion` and `targetSdkVersion` to 35 (for example).
5. Finally, rebuild your project.

Note: Higher `compileSdkVersion` and `targetSdkVersion` will be better.

## Build Your Own Custom `android.jar` (CLI)

Don't want to wait for a prebuilt jar? This repo ships **`hiddenjar`**, a CLI that builds a custom
`android.jar` straight from a **running Android emulator or device** — no rooting, no manual DEX
juggling. It pulls the framework jars over `adb`, converts DEX to `.class`, merges them into the SDK
`android.jar`, strips the merged classes to signature-only stubs (so Gradle's `lint`/unit-test
mockable-jar step accepts the jar — see issue #46), and **verifies the result actually compiles a
hidden API** before finishing.

Unlike the classic recipe, it merges the **whole `$BOOTCLASSPATH`** (including mainline/APEX modules
such as Wi‑Fi, Bluetooth, connectivity, media), so hidden APIs that no longer live in `framework.jar`
are included too.

### Prerequisites

- A JDK (`javac` + `jar`) and `adb` — both standard with Android Studio. The macOS/Linux bash script
  also needs `unzip`; the **Windows PowerShell script needs no `unzip`** (it uses the JDK `jar` tool).
- A **running emulator at API ≥ 34** (recommended) or a device. Older emulator images may ship a
  stripped `framework.jar`; the CLI detects that and tells you to use a newer image.
- `dex-tools` is downloaded and cached automatically on first run.

### Option A — the `hiddenjar` script (macOS/Linux & Windows)

**macOS / Linux** — the bash script:

```bash
# Check your environment (adb, JDK, dex-tools, connected devices)
./cli/hiddenjar doctor

# Build against a running emulator/device (auto-detected); full mainline coverage
./cli/hiddenjar build --api 37

# Faster, framework.jar only (fewer hidden APIs)
./cli/hiddenjar build --only-framework

# Boot an AVD first, build, and install into the SDK (backs up android.jar.orig)
./cli/hiddenjar build --avd Pixel_10_API_37 --install

# Roll back to the stock SDK jar
./cli/hiddenjar restore --api 37
```

**Windows** — the native PowerShell script (`cli\hiddenjar.ps1`, **no Git Bash / WSL required**):

```powershell
# Same commands & flags; runs on built-in Windows PowerShell or PowerShell 7+
powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 doctor
powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 build --api 37
powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 build --only-framework
powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 build --avd Pixel_10_API_37 --install
powershell -ExecutionPolicy Bypass -File cli\hiddenjar.ps1 restore --api 37
```

Common flags (both scripts): `--serial <id>`, `--avd <name>`, `--api <n>`, `--sdk-dir <path>`,
`--output <file>`, `--install`, `--only-framework`, `--keep-bodies`, `--dex-tools <dir>`. Run the script with `help`
for the full list.

### Option B — the Gradle wrapper (macOS/Linux/Windows)

```bash
./gradlew hiddenJarDoctor
./gradlew buildHiddenJar -Papi=37                 # full build
./gradlew buildHiddenJar -Papi=37 -Pinstall       # build + install into the SDK
./gradlew buildHiddenJar -PonlyFramework          # framework.jar only
./gradlew restoreHiddenJar -Papi=37               # roll back
```

Value props: `-Papi -Pserial -Pavd -Poutput -PsdkDir -PdexTools`.
Flag props: `-PonlyFramework -Pinstall -Pkeep`.

> **Windows:** everything works natively — no Git Bash or WSL. The Gradle wrapper (Option B) auto-runs
> the PowerShell script on Windows and the bash script elsewhere, so `gradlew buildHiddenJar -Papi=37`
> works from `cmd`/PowerShell. You only need a JDK and `adb`; set `ANDROID_HOME` if your SDK isn't at
> the default `%LOCALAPPDATA%\Android\Sdk`.

> **Note:** A custom jar only unblocks **compilation**. At runtime, Android 9+ (API 28+) still
> enforces the non-SDK (hidden API) blocklist for non-system apps.

For the full method, findings, and design rationale, see
[`docs/BUILD_CUSTOM_ANDROID_JAR_FROM_EMULATOR.md`](docs/BUILD_CUSTOM_ANDROID_JAR_FROM_EMULATOR.md).

## Resources Helper
![Maven Central](https://img.shields.io/maven-central/v/com.anggrayudi/android-hidden-api.svg)

If you plan to use only Android internal resources rather than internal classes or methods, do:

````gradle
dependencies {
    implementation 'com.anggrayudi:android-hidden-api:X.Y'
}
````

Where `X.Y` is the library version: ![Maven Central](https://img.shields.io/maven-central/v/com.anggrayudi/android-hidden-api.svg)

All versions can be found [here](https://oss.sonatype.org/#nexus-search;gav~com.anggrayudi~android-hidden-api~~~~kw,versionexpand).
To use `SNAPSHOT` version, you need to add this URL to the root Gradle:

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        // add this line
        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
    }
}
```

Here's some example of accessing internal resources:

```java
String accept = InternalAccessor.getString("accept");
float sbar_height = InternalAccessor.getDimension("status_bar_height");
int notif_color = InternalAccessor.getColor("config_defaultNotificationColor");
```

## Contributing

~~If you have your own custom `android.jar` and want to add it to [Google Drive](https://drive.google.com/drive/folders/17oMwQ0xBcSGn159mgbqxcXXEcneUmnph), please create an issue. I will upload it.~~

**Uploading jars is no longer needed** — every developer can build their own with the
[`hiddenjar` CLI](#build-your-own-custom-androidjar-cli). Contributions to the CLI itself
(new flags, fixes, wider API-level coverage) are very welcome via pull request.

## License

    Copyright 2015-2026 Anggrayudi Hardiannico A.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: https://devmaze.wordpress.com/2011/01/18/using-com-android-internal-part-1-introduction
[2]: https://github.com/anggrayudi/android-hidden-api/issues/9
