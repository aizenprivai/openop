# OPENOP — Architecture (as implemented)

This describes what exists in the repository **now** (end of Phase 1).
The full planned architecture is in [`CLAUDE.md`](../CLAUDE.md) §3.

## 1. Modules

```text
openop/
├── app/            Android application (Kotlin, Jetpack Compose, Material 3)
│                   └─ depends on :core-bridge
├── core-bridge/    Android library: builds the Rust library, generates the
│                   UniFFI bindings, exposes the CoreBridge facade
└── rust/           Cargo workspace
    ├── crates/openop-core/   Plain Rust domain logic (no FFI types)
    ├── crates/openop-ffi/    UniFFI exports only → libopenop_ffi.so
    └── uniffi-bindgen/       Binding generator, version-locked to uniffi
```

Dependency direction: `app → core-bridge → openop-ffi → openop-core`.
No layer reaches past the next one.

## 2. Kotlin/Rust bridge (UniFFI)

```text
 Kotlin UI / app code
        │  CoreBridge.coreInfo(): Result<CoreInfo>
        ▼
 core-bridge: CoreBridge (hand-written facade)
        │  generated bindings (package ...core.ffi, build output only)
        ▼
 JNA ── libopenop_ffi.so (openop-ffi, UniFFI scaffolding)
        │
        ▼
 openop-core (plain Rust)
```

### Boundary rules

1. **`openop-core` has no FFI types.** `openop-ffi` converts core types
   into UniFFI records and errors.
2. **Every fallible export returns `Result<_, CoreError>`.**
3. **Only `CoreBridge` touches generated code.** App code uses
   `CoreBridge` and its own types (`CoreInfo`, `CoreBridgeException`),
   never `io.github.aizenprivai.openop.core.ffi.*`.
4. **Every `CoreBridge` call returns `Result`.** Failures are always
   `CoreBridgeException`:
   - `InvalidInput(reason)`: the core rejected the input; the reason
     is safe to show.
   - `Internal(cause)`: the native library is missing or Rust
     panicked. Always a bug.
5. **No native calls on the main thread.** Use
   `withContext(Dispatchers.Default)` or similar.
6. **Never log user content**, in Kotlin or Rust.

### Phase 1 exports

| Rust (`openop-ffi`) | Kotlin (`CoreBridge`) | Purpose |
|---|---|---|
| `core_info() -> CoreInfo` | `coreInfo(): Result<CoreInfo>` | Version and build profile |
| `echo_validated(String) -> Result<String, CoreError>` | `echoValidated(String): Result<String>` | Demonstrates error handling |

## 3. Build integration

`core-bridge/build.gradle.kts` registers two kinds of task:

- **`cargoBuild<Variant>`** runs cargo-ndk in `rust/`. Debug builds
  `arm64-v8a` + `x86_64` with the Cargo dev profile. Release builds
  `arm64-v8a` only with the release profile (LTO, stripped). Output
  goes to `core-bridge/build/rustJniLibs/<variant>/` and is added to
  the variant's `jniLibs`.
- **`generateUniffiBindings`** builds `openop-ffi` for the host and
  runs the workspace's `uniffi-bindgen` in library mode. Output goes to
  `core-bridge/build/generated/uniffi/kotlin/` and is added as a Kotlin
  source directory. The bindings are not committed (decision D8).

Both tasks declare the Rust sources, `Cargo.lock` and
`rust-toolchain.toml` as inputs, so they only rerun when Rust changes.
Machine-specific paths are removed from the Android binaries with
`--remap-path-prefix`.

ABIs (decision D3): the app's `abiFilters` keep only `arm64-v8a` +
`x86_64` (debug) or `arm64-v8a` (release). They also drop the other
ABIs bundled in JNA. All native libraries are 16 KB-aligned; CI checks
this with `scripts/check-native-libs.sh`.

## 4. Release build

- R8 code shrinking and resource shrinking.
- JNA and UniFFI keep rules live in `core-bridge/consumer-rules.pro` and
  apply to any app that depends on `:core-bridge`.
- Signing comes from an untracked `keystore.properties`; without it the
  APK is unsigned.

## 5. Logging and errors

- **Kotlin:** `AppLog` (`app/.../log/AppLog.kt`) is the only logger. Tags
  are `OPENOP/<area>`. `d`/`i` messages only appear in debug builds;
  `w`/`e` always do.
- **Rust:** errors are `thiserror` enums mapped one-to-one to UniFFI
  errors. No logging framework yet. If one is needed, the plan is a
  UniFFI callback into `AppLog`.
- **UI:** show plain-language messages. Technical detail goes to the
  log, never to the screen.

## 6. Privacy and permissions in the build

- The manifest declares **no permissions**.
- `allowBackup="false"`, and `data_extraction_rules.xml` excludes all
  data from cloud backup. Device-to-device transfer is undecided
  (Phase 3).
- No network code and no analytics.

## 7. Supply chain

- Gradle wrapper pinned with a distribution SHA-256; the wrapper JAR is
  validated in CI.
- `gradle/verification-metadata.xml` checks every Gradle dependency
  (SHA-256).
- `rust/Cargo.lock` is committed, and Cargo always runs with `--locked`.
- GitHub Actions are pinned by commit SHA.
