# OPENOP — Development Setup

How to build, test and check OPENOP from a clean checkout.
Verified on Ubuntu 24.04 (x86_64), 2026-11-16.

## 1. Requirements

| Tool | Version | Where it is pinned |
|---|---|---|
| JDK | 21 (17 is the minimum for AGP) | — |
| Android SDK | platform `android-37.0`, build-tools 37.0.0 | `gradle/libs.versions.toml` (`compile-sdk`) |
| Android NDK | 30.0.16248370 (r30) | `gradle/libs.versions.toml` (`ndk`) |
| Rust | 1.100.0 + targets `aarch64-linux-android`, `x86_64-linux-android` | `rust/rust-toolchain.toml` |
| cargo-ndk | 4.1.2 | `gradle/libs.versions.toml` (`cargo-ndk`, read by CI) |
| Gradle | 9.9.1 (downloaded by the wrapper) | `gradle/wrapper/gradle-wrapper.properties` |

About 6 GB of free disk space is needed for the SDK, NDK, Rust toolchain
and Gradle caches. The Android emulator needs several GB more.

## 2. One-time setup

### JDK

```bash
sudo apt install openjdk-21-jdk-headless
```

Android Studio also ships a suitable JDK.

### Android SDK and NDK

Either install Android Studio and use its SDK Manager, or use the
command-line tools from <https://developer.android.com/studio#command-line-tools-only>,
unpacked into `~/Android/Sdk/cmdline-tools/latest`. Then:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-37.0" "build-tools;37.0.0" "ndk;30.0.16248370"
```

Tell Gradle where the SDK is, either with `ANDROID_HOME` or with an
untracked `local.properties` file at the repository root:

```properties
sdk.dir=/home/<you>/Android/Sdk
```

### Rust and cargo-ndk

Install rustup from <https://rustup.rs>. The pinned toolchain and
Android targets install automatically the first time Cargo runs in
`rust/` (from `rust/rust-toolchain.toml`). To install them explicitly:

```bash
cd rust && rustup toolchain install
```

```bash
cargo install cargo-ndk --version 4.1.2 --locked
```

## 3. Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (arm64-v8a + x86_64).

Gradle builds the Rust library and generates the Kotlin bindings by
itself; there is no separate Rust step. See
[`architecture.md`](architecture.md) for how that works.

### Release build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`
(arm64-v8a only, R8-shrunk).

To sign it, create an untracked `keystore.properties` at the repository
root. **Never commit it or the keystore.**

```properties
storeFile=path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## 4. Checks

Run all of these before committing. CI runs the same checks.

```bash
cd rust && cargo fmt --all --check && cargo clippy --workspace --all-targets --locked -- -D warnings && cargo test --workspace --locked
```

```bash
./gradlew assembleDebug assembleRelease lintDebug lintRelease ktlintCheck testDebugUnitTest
```

```bash
scripts/check-native-libs.sh app/build/outputs/apk/release/app-release-unsigned.apk arm64-v8a
```

To fix Kotlin formatting automatically:

```bash
./gradlew ktlintFormat
```

## 5. Instrumented tests (device or emulator)

Connect an Android 12+ device with USB debugging turned on, or start an
x86_64 emulator, then:

```bash
scripts/run-instrumented-tests.sh
```

Use this script rather than calling `connectedDebugAndroidTest` on its own. It
deletes old results, runs the tests, then checks the results. If an APK fails to
install, Gradle can still report `BUILD SUCCESSFUL` with 0 tests run; the
script catches that. To run one module: `scripts/run-instrumented-tests.sh app`.

### Xiaomi / HyperOS devices

HyperOS blocks installs over USB by default
(`INSTALL_FAILED_USER_RESTRICTED`). In **Settings → Additional settings
→ Developer options**, turn on:

- **Install via USB** (may need a Mi account and SIM card), and
- **USB debugging (Security settings)**.

Each install must then be confirmed on the phone before a short
countdown ends. Because Gradle installs several APKs in a row, it can
help to run one module at a time:

```bash
scripts/run-instrumented-tests.sh app
```

or to install and run a test APK by hand:

```bash
adb install -r -t core-bridge/build/outputs/apk/androidTest/debug/core-bridge-debug-androidTest.apk
```

```bash
adb shell am instrument -w io.github.aizenprivai.openop.core.test/androidx.test.runner.AndroidJUnitRunner
```

## 6. Dependency verification

Gradle checks every downloaded dependency against
`gradle/verification-metadata.xml` (SHA-256). A build fails if an
artifact is missing from the file or does not match.

After adding or updating a dependency or plugin, regenerate the file
and review the diff before committing:

```bash
./gradlew --no-configuration-cache --write-verification-metadata sha256 assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease ktlintCheck testDebugUnitTest
```

Include `connectedDebugAndroidTest` (with a device connected) when the
change affects instrumented-test dependencies.

## 7. Updating versions

All versions are pinned. To update one:

1. Check the latest stable version from the official source.
2. Change it in one place: `gradle/libs.versions.toml`,
   `rust/rust-toolchain.toml`, `rust/Cargo.toml`, or the Gradle wrapper.
   The cargo-ndk version is in `gradle/libs.versions.toml`; also update
   the install command in this document.
3. Regenerate dependency verification (section 6).
4. Run all checks (sections 4 and 5).

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| `SDK location not found` | Set `ANDROID_HOME` or create `local.properties` (section 2). |
| `cargo: command not found` during the Gradle build | Make sure `~/.cargo/bin` is on the `PATH` of the shell that starts Gradle, then run `./gradlew --stop`. |
| `no such command: ndk` | Install cargo-ndk (section 2). |
| `Dependency verification failed` | A dependency changed or is new; regenerate (section 6) and review the diff. |
| `INSTALL_FAILED_USER_RESTRICTED` | See the Xiaomi / HyperOS notes (section 5). |
