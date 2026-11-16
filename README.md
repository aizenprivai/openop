# OPENOP

OPENOP is a planned open-source, offline-first, all-in-one toolbox for
Android 12 and later. It will bring a file manager, notes, device
information, small utilities and local AI into one app, with no account,
no telemetry and no cloud dependency.

## Status

**Early development — nothing usable yet.**

Phase 1 (repository and build foundation) is complete, pending
review: the app builds, launches and calls the Rust core through UniFFI.
None of the planned modules are implemented yet. See:

- [`CLAUDE.md`](CLAUDE.md) — full project specification and roadmap
- [`docs/phase-1-plan.md`](docs/phase-1-plan.md) — Phase 1 plan and progress
- [`docs/architecture.md`](docs/architecture.md) — architecture as implemented

## Planned modules

Dashboard · Files · Notes · Device · Tools · Local AI
(LiteRT-LM and GGUF via llama.cpp)

## Technology

- Kotlin + Jetpack Compose + Material 3 (UI)
- Rust (shared core), connected to Kotlin with UniFFI
- Minimum Android version: 12 (API 31)

## Building

Requirements: JDK 21, Android SDK with NDK r30, Rust (rustup) and
cargo-ndk. Full setup: [`docs/development.md`](docs/development.md).

```bash
./gradlew assembleDebug
```

Gradle also builds the Rust core and generates the Kotlin bindings.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE). Third-party components
keep their own licenses.
