# OPENOP

OPENOP is a planned open-source, offline-first, all-in-one toolbox for
Android 12 and later. It will bring a file manager, notes, device
information, small utilities and local AI into one app, with no account,
no telemetry and no cloud dependency.

## Status

**Early development — nothing usable yet.**

The project is in Phase 1 (repository and build foundation). None of the
planned modules are implemented. See:

- [`CLAUDE.md`](CLAUDE.md) — full project specification and roadmap
- [`docs/phase-1-plan.md`](docs/phase-1-plan.md) — current phase plan

## Planned modules

Dashboard · Files · Notes · Device · Tools · Local AI
(LiteRT-LM and GGUF via llama.cpp)

## Technology

- Kotlin + Jetpack Compose + Material 3 (UI)
- Rust (shared core), connected to Kotlin with UniFFI
- Minimum Android version: 12 (API 31)

## Building

Build instructions will be added in `docs/development.md` once the
Android and Rust build is in place.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE). Third-party components
keep their own licenses.
