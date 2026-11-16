# Contributing to OPENOP

OPENOP is in early development. This guide will grow as the build and
tooling are set up.

## Before you start

- Read [`CLAUDE.md`](CLAUDE.md). It is the project specification and
  lists which decisions are made and which are still open.
- Features, scope, architecture and new dependencies need the project
  owner's approval before implementation. Open an issue to propose them.

## Ground rules

- Offline-first: no network access unless a feature explicitly needs it.
- No analytics, tracking or proprietary SDKs.
- Request Android permissions only when a feature needs them.
- Never log note contents, conversation contents or other user data.
- Keep changes small and reviewable.
- Do not claim tests pass unless you ran them.

## Formatting and checks

Planned (set up during Phase 1):

- Rust: `cargo fmt`, `cargo clippy -- -D warnings`, `cargo test`
- Kotlin: ktlint, Android Lint, unit and instrumented tests

## Licensing

Contributions are accepted under the Apache License 2.0, the project's
license (see [`LICENSE`](LICENSE)).
