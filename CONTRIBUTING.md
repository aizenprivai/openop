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

## Setup

See [`docs/development.md`](docs/development.md).

## Formatting and checks

Run before every commit (CI runs the same):

- Rust: `cargo fmt --all --check`, `cargo clippy --workspace --all-targets --locked -- -D warnings`, `cargo test --workspace --locked`
- Kotlin/Android: `./gradlew assembleDebug assembleRelease lintDebug lintRelease ktlintCheck testDebugUnitTest`
- On a device: `scripts/run-instrumented-tests.sh`

Fix Kotlin formatting with `./gradlew ktlintFormat`.

## Dependencies

New or updated dependencies need project-owner approval (see
`CLAUDE.md` §8). After any change, regenerate
`gradle/verification-metadata.xml` and review the diff
(`docs/development.md` §6).

## Architecture rules

See [`docs/architecture.md`](docs/architecture.md). In short: only
`CoreBridge` touches the generated UniFFI code, no native calls on the
main thread, and never log user content.

## Licensing

Contributions are accepted under the Apache License 2.0, the project's
license (see [`LICENSE`](LICENSE)).
