# OPENOP — Phase 1 Implementation Plan

> **Phase:** 1 — Repository and development foundation
> **Status:** Decisions D1–D9 approved 2026-11-16 (§10). Implementation
> still needs explicit authorization, and D1 needs the GitHub username.
> **Date:** 2026-11-16
> **Scope source:** `CLAUDE.md` §10 (Phase 1) and §17

This document is a plan only. It does not authorize creating the
repository or writing code. Section 10 lists the decisions the project
owner needs to make before implementation can start.

------------------------------------------------------------------------

## 1. Goal

When Phase 1 is done, a clean checkout builds with documented steps
into a small Android app that:

- launches on an Android 12+ (API 31+) device or emulator,
- calls a Rust function through UniFFI and shows the result,
- handles an error returned by Rust without crashing,

with formatting, linting, tests and CI checks running from the start.

Phase 1 delivers **no user-facing features**. The single screen is a
developer placeholder and must say so.

### Out of scope for Phase 1

- Bottom navigation, module screens, design system (Phase 2).
- Room, settings, permission coordinator (Phase 3).
- Any Dashboard, Files, Notes, Device, Tools or AI functionality.
- LiteRT-LM, llama.cpp and Hugging Face integration (Phase 7).
- F-Droid metadata and release signing for distribution (Phase 10).
- Creating or pushing to a GitHub repository (owner's action).

------------------------------------------------------------------------

## 2. Proposed repository tree

```text
openop/
├── CLAUDE.md                     Project specification (exists)
├── README.md                     Overview + quick build instructions
├── LICENSE                       Apache-2.0 (pending decision D2)
├── CONTRIBUTING.md               Workflow, formatting, commit conventions
├── .gitignore
├── .editorconfig
│
├── settings.gradle.kts           Includes :app and :core-bridge
├── build.gradle.kts              Root build (plugins declared, not applied)
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml        Single source of truth for versions
│   └── wrapper/                  Gradle wrapper (checksum-verified)
├── gradlew, gradlew.bat
│
├── app/                          Android application module
│   ├── build.gradle.kts
│   ├── proguard-rules.pro        Keep rules for JNA / UniFFI
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/<package>/
│       │       ├── MainActivity.kt
│       │       ├── ui/BridgeDemoScreen.kt
│       │       └── log/AppLog.kt
│       ├── test/                 JVM unit tests
│       └── androidTest/          Instrumented tests (calls into Rust)
│
├── core-bridge/                  Android library module
│   ├── build.gradle.kts          Runs cargo-ndk + uniffi-bindgen
│   └── src/main/kotlin/<package>/core/
│       └── CoreBridge.kt         Thin hand-written Kotlin facade
│                                 (generated bindings go in build/,
│                                  not committed — see D8)
│
├── rust/
│   ├── Cargo.toml                Workspace
│   ├── Cargo.lock                Committed (reproducibility)
│   ├── rust-toolchain.toml       Pins Rust version + Android targets
│   ├── rustfmt.toml
│   ├── crates/
│   │   ├── openop-core/          Pure Rust domain logic, no FFI types
│   │   │   ├── Cargo.toml
│   │   │   └── src/lib.rs
│   │   └── openop-ffi/           UniFFI exports only; cdylib
│   │       ├── Cargo.toml
│   │       └── src/lib.rs
│   └── uniffi-bindgen/           Tiny bin crate; version-locked bindgen
│       ├── Cargo.toml
│       └── src/main.rs
│
├── docs/
│   ├── phase-1-plan.md           This document
│   ├── development.md            Full setup guide
│   └── architecture.md           Layers, bridge boundary, conventions
│
└── .github/
    └── workflows/
        └── ci.yml
```

### Why this layout

- **`openop-core` vs `openop-ffi`:** the core crate stays plain Rust
  and fully testable on the host. Only `openop-ffi` knows about UniFFI.
  This keeps the boundary narrow (CLAUDE.md §3.5) and keeps FFI
  details out of domain code.
- **`core-bridge` as its own Android module:** the native library,
  generated bindings and the JNA dependency stay in one place. Later
  modules depend on `CoreBridge`, not on generated code directly.
- **`uniffi-bindgen` in the workspace:** this is the method UniFFI
  documents. The bindgen version is locked to the `uniffi` crate
  version through `Cargo.lock`, so the two cannot drift apart.

------------------------------------------------------------------------

## 3. Toolchain versions to verify

CLAUDE.md §8 forbids hardcoding versions from stale information. The
values below are **candidates** found by web search on 2026-11-16.
Several sources conflicted, so **each must be confirmed against the
official source on the day implementation starts**, then pinned.

| Tool | Candidate | Confidence | Verify at | Pinned in |
|---|---|---|---|---|
| Android Gradle Plugin | 9.2.x (9.3 RC seen) | Low — undated source | developer.android.com/build/releases/gradle-plugin | `libs.versions.toml` |
| Gradle | Whatever the chosen AGP requires | — | AGP compatibility table | `gradle-wrapper.properties` + SHA-256 |
| JDK | Whatever the chosen AGP requires (17 minimum) | — | AGP release notes | `development.md`, CI |
| Kotlin | 2.4.20 | Medium — official docs page | kotlinlang.org releases | `libs.versions.toml` |
| Compose BOM | 2026.08.00 (Compose 1.12) or newer | Medium | Android Developers blog / BOM mapping | `libs.versions.toml` |
| compileSdk / targetSdk | 37 (Compose 1.12 requires compileSdk 37) | Medium | SDK platform release notes | `app/build.gradle.kts` |
| minSdk | **31** | Approved | — | `app/build.gradle.kts` |
| Android NDK | r29 stable (r30 beta seen; check for a newer LTS) | Medium | developer.android.com/ndk/downloads | `ndkVersion` in Gradle |
| Rust | 1.99.1 (1.100 may be out) | Medium | blog.rust-lang.org | `rust-toolchain.toml` |
| UniFFI | 0.32.0 | Medium | github.com/mozilla/uniffi-rs CHANGELOG | `Cargo.toml` (exact `=` version) |
| cargo-ndk | 4.1.2 (may be newer) | Low | github.com/bbqsrc/cargo-ndk releases | `development.md`, CI (`--locked --version`) |
| JNA | Version that matches the UniFFI release | — | UniFFI Kotlin docs | `libs.versions.toml` |

**Version policy for Phase 1:** use the latest stable version of each
tool unless the tools are incompatible with each other. When that
happens, choose the newest combination that works together and record
why.

------------------------------------------------------------------------

## 4. Build integration (Gradle + Cargo + NDK)

### 4.1 Build flow

```text
./gradlew :app:assembleDebug
  │
  └─ :core-bridge:preBuild depends on
       │
       ├─ cargoBuild<Variant>      (Exec task)
       │    cargo ndk -t <abis> -o build/rustJniLibs/<variant>
       │              build --profile <debug|release>
       │              -p openop-ffi
       │    → libopenop_ffi.so per ABI
       │
       └─ generateUniffiBindings   (Exec task)
            cargo run -p uniffi-bindgen -- generate
                  --library <host-or-target .so> --language kotlin
                  --out-dir build/generated/uniffi/kotlin
            → Kotlin bindings added as a source set
```

- Gradle declares task inputs and outputs (`rust/**`, `Cargo.lock`,
  `rust-toolchain.toml`), so Cargo only runs when Rust sources change.
- The NDK path comes from the Android SDK (`android.ndkDirectory`) and
  is passed to cargo-ndk, so developers don't need to set
  `ANDROID_NDK_HOME` by hand.
- Cargo's own incremental build cache under `rust/target/` is reused.

### 4.2 Recommended glue: plain Gradle `Exec` tasks + cargo-ndk

| Option | For | Against |
|---|---|---|
| **Plain `Exec` tasks + cargo-ndk** (recommended) | No extra Gradle plugin; every step visible in the build script; cargo-ndk is widely used, including by F-Droid apps | ~60–100 lines of build script to maintain |
| Mozilla `rust-android-gradle` plugin | Less script | Another third-party plugin to review; update cadence has varied; less control over flags needed for reproducibility |
| Hand-written `CMake`/`ndk-build` wrappers | — | Wrong tool for Rust; adds complexity |

See decision **D4**.

### 4.3 Rust build settings

`rust/Cargo.toml` workspace profiles:

```toml
[profile.release]
lto = true
codegen-units = 1
strip = "symbols"   # debug symbols kept separately if needed later
panic = "unwind"    # UniFFI catches panics at the boundary → Kotlin error
```

Reproducibility (needed for F-Droid and helpful for GitHub releases):

- `--remap-path-prefix` set by the Gradle task so absolute build paths
  aren't embedded in the `.so`.
- Exact pinned toolchain (`rust-toolchain.toml`) and NDK version.
- `Cargo.lock` committed; Cargo invoked with `--locked`.
- `SOURCE_DATE_EPOCH` respected where the toolchain honours it.

### 4.4 Build variants

| Variant | Purpose | Notes |
|---|---|---|
| `debug` | Development, CI, tests | Debuggable; Rust `dev` profile; includes emulator ABI (D3) |
| `release` | Release builds | R8 minify + resource shrink; Rust `release` profile; **arm64-v8a only**; signing config read from a local, untracked properties file or environment variables — never committed |

No product flavors in Phase 1. If F-Droid later needs a separate
configuration (CLAUDE.md §9.4), it will be proposed as its own
decision.

------------------------------------------------------------------------

## 5. Kotlin/Rust bridge (UniFFI)

### 5.1 Approach

- **Proc-macro mode** (`#[uniffi::export]`, `#[derive(uniffi::Record)]`,
  `#[derive(uniffi::Error)]`) rather than separate UDL files. It's the
  direction UniFFI is heading and keeps the definitions next to the
  code.
- Bindings are generated in **library mode** from the compiled `.so`.
- UniFFI's Kotlin bindings use **JNA** at runtime. JNA is a required,
  unavoidable dependency of UniFFI on Kotlin (see D7).

### 5.2 Phase 1 demonstration interface

Small on purpose. It proves the four things the exit criteria need:
calling Rust, returning a structured value, returning an error, and
surviving a Rust panic.

```rust
// openop-ffi — illustrative, final names decided at implementation

#[derive(uniffi::Record)]
pub struct CoreInfo {
    pub core_version: String,   // from CARGO_PKG_VERSION
    pub build_profile: String,  // "debug" | "release"
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum CoreError {
    #[error("invalid input: {reason}")]
    InvalidInput { reason: String },
}

#[uniffi::export]
pub fn core_info() -> CoreInfo { /* … */ }

/// Returns Err(InvalidInput) for empty input — used to test error mapping.
#[uniffi::export]
pub fn echo_validated(input: String) -> Result<String, CoreError> { /* … */ }
```

In Kotlin, `CoreBridge` wraps these calls and returns
`Result<CoreInfo>` / `Result<String>`. Generated types never leak past
`core-bridge`.

### 5.3 Boundary rules (go into `docs/architecture.md`)

1. `openop-core` has no FFI types; `openop-ffi` turns core types into
   FFI types.
2. Every exported function returns `Result<_, CoreError>` unless it
   can't fail.
3. Rust never panics on purpose across the boundary. Panics are bugs,
   and UniFFI turns them into Kotlin exceptions, which `CoreBridge`
   catches.
4. Blocking Rust calls are never made on the main thread (relevant from
   Phase 3 onward; Phase 1 calls are trivial).
5. Rust never logs user content (CLAUDE.md §7.3).

------------------------------------------------------------------------

## 6. Supported ABIs

| ABI | Release | Debug | Reason |
|---|---|---|---|
| `arm64-v8a` | ✅ | ✅ | Approved primary target; almost every Android 12+ device |
| `x86_64` | ❌ | ✅ (proposed, D3) | Lets the Rust library run on the fast x86_64 emulator in CI and on developers' PCs |
| `armeabi-v7a` | ❌ | ❌ | Not approved; few Android 12+ devices are 32-bit only |
| `x86` | ❌ | ❌ | Not useful |

**16 KB memory pages:** newer Android devices can use 16 KB memory
pages. Native libraries must be built with 16 KB-aligned ELF segments.
Recent NDKs (r28+) do this by default; Phase 1 checks it with
`llvm-readelf` in CI so it can't regress.

------------------------------------------------------------------------

## 7. Logging and error conventions

**Kotlin**

- `AppLog`: a thin wrapper over `android.util.Log` with a fixed tag
  prefix (`OPENOP/<area>`).
- Debug builds log at debug level and above; release builds log
  warnings and errors only (R8 removes lower levels).
- Never log note text, conversation text, file contents or full file
  paths that could identify the user.
- Errors shown to users get plain-language messages. Technical detail
  goes to the log, not the screen.

**Rust**

- Errors are typed enums (`thiserror`) that map one-to-one to UniFFI
  errors.
- No logging framework in Phase 1. If one is needed later, the plan is
  a UniFFI callback interface that sends Rust log lines into `AppLog`.
  That avoids adding an Android logging crate.

------------------------------------------------------------------------

## 8. Formatting, linting and tests

### 8.1 Checks

| Area | Tool | New dependency? |
|---|---|---|
| Rust formatting | `rustfmt` (`cargo fmt --check`) | No — part of toolchain |
| Rust lint | `clippy` (`-D warnings`) | No — part of toolchain |
| Rust tests | `cargo test` (host) | No |
| Kotlin lint | Android Lint (`lintDebug`, warnings as errors for new code) | No — part of AGP |
| Kotlin formatting | ktlint *or* ktfmt *or* none (D5) | Yes, if chosen |
| Kotlin unit tests | JUnit 4 (`testDebugUnitTest`) | Yes (EPL-1.0, test-only) |
| Instrumented tests | AndroidX Test + Compose UI Test | Yes (Apache-2.0, test-only) |

### 8.2 Initial tests

**Rust (`openop-core`, `openop-ffi`) — host unit tests**

- `core_info` returns the crate version and correct profile.
- `echo_validated("")` returns `InvalidInput`.
- `echo_validated("abc")` returns `"abc"`.

**Kotlin JVM unit tests (`app`)**

- `CoreBridge` result mapping logic, tested with a fake (no native
  library on the host JVM).

**Instrumented tests (emulator / device) — the key Phase 1 test**

- `BridgeInstrumentedTest`: loads the real `.so`, calls `core_info()`
  and asserts the version matches the version embedded at build time.
- `BridgeErrorInstrumentedTest`: calls `echo_validated("")` and asserts
  a mapped error, not a crash.
- `BridgeDemoScreenTest` (Compose UI test): the placeholder screen shows
  the Rust core version text.

------------------------------------------------------------------------

## 9. CI workflow (GitHub Actions)

`.github/workflows/ci.yml`, runs on pushes and pull requests:

| Job | Runner | Steps |
|---|---|---|
| `rust` | ubuntu-latest | Install pinned toolchain → `cargo fmt --check` → `cargo clippy --workspace -- -D warnings` → `cargo test --workspace --locked` |
| `android` | ubuntu-latest | JDK + SDK + pinned NDK + Rust Android targets + cargo-ndk (pinned) → `./gradlew assembleDebug lintDebug testDebugUnitTest` → 16 KB alignment check → upload debug APK as a workflow artifact |
| `instrumented` | ubuntu-latest (KVM) | x86_64 emulator, API 31 **and** latest API → `./gradlew connectedDebugAndroidTest` |

- Caches: Gradle (`actions/setup-java` / `gradle/actions/setup-gradle`)
  and Cargo (`~/.cargo`, `rust/target`).
- Emulator: `reactivecircus/android-emulator-runner`, a third-party
  action, needs approval (D6).
- Third-party actions are pinned by commit SHA, not by tag.
- Gradle wrapper checksum checked (`gradle/actions/wrapper-validation`).
- No secrets are needed in Phase 1. No release job yet.

------------------------------------------------------------------------

## 10. Decisions needed before implementation

| # | Decision | Options | Recommendation | Consequence |
|---|---|---|---|---|
| **D1** | Application ID / Kotlin package | e.g. `org.openop.app` (needs a domain you control) · `io.github.<username>.openop` · other | `io.github.<username>.openop` unless you own a domain | **Can't be changed after the first public release.** It identifies the app on F-Droid and on devices. |
| **D2** | Add the Apache-2.0 `LICENSE` at repository creation | Apache-2.0 now · GPLv3 comparison first · defer | Apache-2.0 now | Without a license file the code isn't legally open source. |
| **D3** | x86_64 ABI in **debug builds only** | Yes · No (arm64 only, CI uses slower ARM emulation or real devices) | Yes | Fast emulator tests in CI and on most developer PCs; release APK stays arm64-only. |
| **D4** | Gradle↔Cargo glue | Plain Exec tasks + cargo-ndk · Mozilla rust-android-gradle plugin | Exec + cargo-ndk | One less third-party plugin; full control over reproducibility flags. |
| **D5** | Kotlin formatter | ktlint · ktfmt · none (Android Lint only) | ktlint (via its Gradle plugin) | Consistent style for contributors; one more build dependency (MIT). |
| **D6** | CI emulator action | `reactivecircus/android-emulator-runner` · hand-written emulator script | The action, pinned by SHA | Standard and reliable; it's a third-party action (Apache-2.0). |
| **D7** | Accept JNA as a runtime dependency | Accept · (no real alternative with UniFFI on Kotlin) | Accept, used under its Apache-2.0 licence option | Required by UniFFI. JNA's AAR contains a prebuilt native library (`libjnidispatch.so`); **F-Droid's handling of this must be checked in the audit**. Many F-Droid apps already ship JNA from Maven Central, but that needs confirming for OPENOP. |
| **D8** | Commit generated UniFFI Kotlin bindings? | Generate at build time, don't commit · commit them | Don't commit | Can't go out of sync with Rust; F-Droid sees only real sources. Every build needs the Rust toolchain (it does anyway for the `.so`). |
| **D9** | Gradle dependency verification (`verification-metadata.xml`) | Enable now · later · never | Enable in Phase 1 | Protects against tampered dependencies; every version bump has to update checksums. |

### 10.1 Approved answers (2026-11-16)

The project owner accepted every recommendation above:

- **D1:** Application ID `io.github.<username>.openop`. **Still needed:**
  the GitHub username to fill it in.
- **D2:** Add the Apache-2.0 `LICENSE` when the repository is created.
- **D3:** x86_64 in debug builds only; release builds are arm64-v8a only.
- **D4:** Plain Gradle `Exec` tasks + cargo-ndk.
- **D5:** ktlint (via its Gradle plugin).
- **D6:** `reactivecircus/android-emulator-runner`, pinned by commit SHA.
- **D7:** JNA accepted, used under Apache-2.0; F-Droid check still
  required.
- **D8:** Generated UniFFI bindings are built at build time and not
  committed.
- **D9:** Gradle dependency verification enabled in Phase 1.

------------------------------------------------------------------------

## 11. Dependency inventory (Phase 1)

Every item follows the CLAUDE.md §8 review requirements. Licences are
listed as currently understood. **The formal audit happens in
Phase 9**, but anything doubtful is flagged here.

| Dependency | Needed for | Licence | Runtime / build / test | F-Droid notes |
|---|---|---|---|---|
| Android Gradle Plugin | Android build | Apache-2.0 | Build | Standard |
| Kotlin + Kotlin Gradle plugin | Language | Apache-2.0 | Build + runtime stdlib | Standard |
| AndroidX Activity Compose, Core KTX | Activity host | Apache-2.0 | Runtime | Standard |
| Jetpack Compose (BOM) + Material 3 | Placeholder UI | Apache-2.0 | Runtime | Standard |
| `uniffi` (Rust crate) | Bridge | **MPL-2.0** | Compiled into `.so` | FLOSS. MPL-2.0 is file-level copyleft; compatible with Apache-2.0 distribution, but changes to UniFFI's own files must stay MPL. Must be listed in `NOTICE`. |
| `thiserror` | Rust error types | MIT / Apache-2.0 | Compiled into `.so` | Standard |
| JNA (`net.java.dev.jna:jna`, AAR) | UniFFI runtime | Apache-2.0 / LGPL-2.1 (dual) | Runtime | Contains a prebuilt `.so` — check in audit (D7) |
| cargo-ndk | Cross-compiling Rust | MIT / Apache-2.0 | Build tool (installed, not shipped) | Used by other F-Droid apps |
| ktlint (if D5) | Formatting | MIT | Build | Build-only |
| JUnit 4 | Unit tests | EPL-1.0 | Test-only | Not shipped |
| AndroidX Test, Compose UI Test | Instrumented tests | Apache-2.0 | Test-only | Not shipped |

No network access, analytics, Google Play Services or proprietary SDKs
are introduced. The app manifest declares **no permissions** in
Phase 1.

------------------------------------------------------------------------

## 12. Work breakdown (reviewable increments)

Each increment ends with a buildable project and a short report to the
project owner (what changed, what was run, what passed or failed).

| # | Increment | Done when |
|---|---|---|
| 1.1 | `git init`, `.gitignore`, `.editorconfig`, `README.md` stub, `LICENSE` (D2), `CONTRIBUTING.md` stub | Repository exists locally with a first commit |
| 1.2 | Rust workspace: `openop-core`, `openop-ffi`, `uniffi-bindgen`, toolchain pin, demo functions + host tests | `cargo fmt --check`, `clippy`, `test` all pass on host |
| 1.3 | Android project: Gradle wrapper, version catalog, `:app` with an empty Compose activity | `./gradlew assembleDebug` succeeds; app launches on an API 31 emulator |
| 1.4 | `:core-bridge`: cargo-ndk + bindgen tasks, `CoreBridge` facade, placeholder screen shows Rust result, instrumented tests | Rust version visible on screen; instrumented tests pass on emulator |
| 1.5 | `AppLog`, error conventions, release build (R8 + keep rules) | `assembleRelease` builds an arm64-only APK that runs and calls Rust |
| 1.6 | CI workflow (`rust`, `android`, `instrumented` jobs) | Workflow file valid; passes once the owner pushes to GitHub |
| 1.7 | `docs/development.md`, `docs/architecture.md`, README build section | A fresh-machine build following only the docs succeeds |

------------------------------------------------------------------------

### 12.1 Progress

| # | State | Notes |
|---|---|---|
| 1.1 | ✅ Committed (`d063b91`) | `git init -b main`, `.gitignore`, `.gitattributes`, `.editorconfig`, `LICENSE` (copied from the system's Apache-2.0 text, SHA-256 `cfc7749b…3d30`), `README.md`, `CONTRIBUTING.md`. Repo-local git identity set. |
| 1.2 | ✅ Done | Rust 1.100.0 pinned; UniFFI `=0.32.2`, thiserror 2.0.21 (checked on crates.io 2026-11-16). On host: `cargo fmt --check`, `clippy -D warnings` and `cargo test` pass (8 tests); release build OK; Kotlin bindings generate from the `.so` (`CoreInfo`, `CoreException.InvalidInput`, `coreInfo()`, `echoValidated()`; 25 JNA references, as expected). |
| 1.3+ | Toolchain ready | Installed 2026-11-16: OpenJDK 21.0.13 (apt); Android SDK in `~/Android/Sdk` with cmdline-tools 16111833, platform-tools, build-tools 37.0.0, platform android-37.0, **NDK r30 (30.0.16248370)**; cargo-ndk 4.1.2. AGP 9.5.1 confirmed as latest stable on Google Maven. Cross-compiled `openop-ffi` for arm64-v8a and x86_64: all LOAD segments 16 KB-aligned (0x4000). |

Testing device: instrumented tests run on the owner's Android 12+ phone
over USB, not an emulator. The disk has too little free space for
emulator images.

Notes for step 1.4:

- Generated bindings default to package `uniffi.openop_ffi`. Set the
  package from the app ID in `rust/crates/openop-ffi/uniffi.toml`.
- Pass `--no-format` to the bindgen task. Otherwise it tries to run a
  ktlint executable that isn't there; ktlint runs as a Gradle check
  instead.
- The Android targets (`aarch64-linux-android`, `x86_64-linux-android`)
  were installed automatically through `rust-toolchain.toml`.

D1 application ID: `io.github.aizenprivai.openop` (from the GitHub
profile the owner gave), to be confirmed before step 1.3.

## 13. Acceptance criteria (Phase 1 exit)

From CLAUDE.md §10, made concrete:

- [ ] A clean checkout builds with only the steps in
      `docs/development.md` (checked on a fresh environment or
      container).
- [ ] The debug APK installs and launches on an API 31 emulator and on
      a recent-API emulator.
- [ ] The placeholder screen shows the Rust core version obtained
      through UniFFI.
- [ ] A Rust error reaches Kotlin as a handled error, not a crash
      (instrumented test).
- [ ] The release APK contains only `arm64-v8a` native libraries, and
      they are 16 KB-aligned.
- [ ] `cargo fmt`, `clippy`, `cargo test`, Android Lint, unit tests and
      instrumented tests all pass, and they were actually run.
- [ ] CI workflow runs the agreed checks. Fully verifying it needs the
      GitHub repository, which is the owner's action.
- [ ] No permissions in the manifest; no network access.
- [ ] Every Phase 1 dependency is listed with its licence (§11).
- [ ] Every toolchain version is pinned and recorded with the date it
      was checked.

------------------------------------------------------------------------

## 14. Risks and how they're handled

| Risk | Handling |
|---|---|
| Toolchain versions found by search are wrong or out of date | Every version is re-checked against the official source at implementation start (§3) |
| AGP / Kotlin / Compose versions don't work together | Use the newest working combination; record why |
| F-Droid build server needs Rust + NDK | Common for F-Droid apps; pinned versions and documented build make it repeatable. Confirmed in the Phase 9 / 10 audit |
| JNA's prebuilt native library vs F-Droid policy | Flagged now (D7); checked early, not at release time |
| UniFFI breaking changes between versions | Exact version pin; bindgen locked to the same version via the workspace |
| Reproducible builds harder with native code | Path remapping, pinned toolchains, locked dependencies from day one |
| CI emulator flakiness | Retries on emulator boot only, never on test failures |

------------------------------------------------------------------------

## 15. Approval question

Decisions D1–D9 are answered (§10.1). Implementation of Phase 1 begins,
one increment at a time starting with 1.1, once the project owner
explicitly authorizes it and provides the GitHub username for D1.
