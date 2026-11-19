# OPENOP — Architecture (as implemented)

This describes what exists in the repository **now** (end of Phase 2).
The full planned architecture is in [`CLAUDE.md`](../CLAUDE.md) §3.

## 1. Modules

```text
openop/
├── app/                 Android application: the app shell (navigation bar/rail,
│                        Navigation 3), MainActivity, More screen, AppLog
├── feature/
│   ├── dashboard/       Dashboard (Phase 2: shortcuts + Rust core footer)
│   ├── files/ notes/ device/ tools/ ai/
│                        Module entry screens (Phase 2: placeholders)
├── core-ui/             Design system: OpenopTheme, Spacing, components,
│                        destination visuals, icons
├── core-navigation/     Pure Kotlin navigation model (no Android)
├── core-bridge/         Android library: builds the Rust library, generates the
│                        UniFFI bindings, exposes the CoreBridge facade
├── build-logic/         Gradle convention plugins (openop.android.*, openop.rust-bridge)
└── rust/                Cargo workspace
    ├── crates/openop-core/   Plain Rust domain logic (no FFI types)
    ├── crates/openop-ffi/    UniFFI exports only → libopenop_ffi.so
    └── uniffi-bindgen/       Binding generator, version-locked to uniffi
```

### Dependency rules

```text
:app ──► :feature:* ──► :core-ui ──► :core-navigation
  │                                   ▲
  ├──────────────────────────────────┘
  └──► :core-bridge ──► rust (openop-ffi → openop-core)
```

- Features never depend on each other or on `:core-bridge`. `:app` is the
  only module that knows every feature and wires them together.
- `:core-ui` depends on `:core-navigation` (pure Kotlin) so one mapping holds
  each destination's label, icon and planned phase (`DestinationVisuals`).
- Rust: no layer reaches past the next one.

## Navigation (Phase 2)

Decision P2-D1: phones show a **bottom bar** with Dashboard, Files, Notes,
AI and More (More lists Device and Tools); wide windows show a **navigation
rail** with all six areas. `NavigationSuiteScaffold` chooses bar or rail from
the window size.

- **Model:** `:core-navigation` holds an immutable `NavState`: the selected
  tab plus one back stack per tab. Operations: `select(tab)` (bar tap;
  reselect resets the tab), `open(destination)` (rail tap, Dashboard
  shortcut, More row; opening what is shown changes nothing), `back()`
  (pop, else Dashboard, else leave the app). Rules: `docs/phase-2-plan.md` §4.
- **Display:** `OpenopApp` feeds `NavState.displayed` to Navigation 3's
  `NavDisplay`. When another tab is selected, the Dashboard stack sits
  underneath, so the back gesture previews where back goes.
- **Saving:** `rememberSaveable` with a custom saver that stores
  `NavStateCodec` text. It survives recreation and process death. Corrupt or
  unknown saved state falls back to the start state. No kotlinx.serialization.
- **Adding a destination:** add it to `Destination` and `TopLevel` (if it is a
  tab), give it visuals in `DestinationVisuals`, add its screen to
  `OpenopApp`'s entry provider, and extend the `:core-navigation` tests.

## Design system

See [`design-system.md`](design-system.md).

## Build logic

Shared Gradle configuration lives in `build-logic/` (an included build):

| Plugin | Applies to | Configures |
|---|---|---|
| `openop.android.application` | `:app` | SDK levels, Java 17, strict lint, ktlint |
| `openop.android.library` | Android libraries | Same, plus JUnit 4 |
| `openop.android.compose` | Compose modules | Compose compiler, BOM, Material 3, UI test and accessibility test setup, lifecycle alignment |
| `openop.kotlin.jvm` | `:core-navigation` | Pure Kotlin, Java 17, ktlint, JUnit 4 |
| `openop.ktlint` | all of the above | ktlint version, generated-code exclude |
| `openop.rust-bridge` | `:core-bridge` | cargo-ndk build, UniFFI bindings, `EXPECTED_CORE_VERSION` |

Plugin versions come from `gradle/libs.versions.toml` through the root
build's `plugins { … apply false }` block.

## Kotlin/Rust bridge (UniFFI)

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

## Rust build integration

The `openop.rust-bridge` convention plugin (`build-logic/`) registers two kinds of task in `:core-bridge`:

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

## Release build

- R8 code shrinking and resource shrinking.
- JNA and UniFFI keep rules live in `core-bridge/consumer-rules.pro` and
  apply to any app that depends on `:core-bridge`.
- Signing comes from an untracked `keystore.properties`; without it the
  APK is unsigned.

## Logging and errors

- **Kotlin:** `AppLog` (`app/.../log/AppLog.kt`) is the only logger. Tags
  are `OPENOP/<area>`. `d`/`i` messages only appear in debug builds;
  `w`/`e` always do.
- **Rust:** errors are `thiserror` enums mapped one-to-one to UniFFI
  errors. No logging framework yet. If one is needed, the plan is a
  UniFFI callback into `AppLog`.
- **UI:** show plain-language messages. Technical detail goes to the
  log, never to the screen.

## Privacy and permissions in the build

- The manifest declares **no permissions**.
- `allowBackup="false"`, and `data_extraction_rules.xml` excludes all
  data from cloud backup. Device-to-device transfer is undecided
  (Phase 3).
- No network code and no analytics.

## Supply chain

- Gradle wrapper pinned with a distribution SHA-256; the wrapper JAR is
  validated in CI.
- `gradle/verification-metadata.xml` checks every Gradle dependency
  (SHA-256).
- `rust/Cargo.lock` is committed, and Cargo always runs with `--locked`.
- GitHub Actions are pinned by commit SHA.
