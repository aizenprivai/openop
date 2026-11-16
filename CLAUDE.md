# OPENOP --- Project Roadmap and Feature Specification

> **Document:** `CLAUDE.md`\
> **Project:** OPENOP\
> **Document purpose:** Canonical project context for AI coding
> assistants and contributors\
> **Status:** Architecture approved; implementation not yet authorized\
> **Platform:** Android 12+ (minimum API 31)\
> **Distribution targets:** GitHub and F-Droid\
> **License recommendation:** Apache License 2.0\
> **Project approach:** Offline-first, open-source, modular,
> privacy-respecting

------------------------------------------------------------------------

## 1. Project overview

### 1.1 What is OPENOP?

OPENOP is an open-source, offline-first, all-in-one Android toolbox. It
brings practical device utilities, file management, note-taking, device
information, and local AI into one cohesive application.

The goal is to reduce the need to install many separate utility
applications while keeping the app modular, maintainable, transparent,
and useful without an internet connection.

OPENOP should feel like one integrated product rather than a collection
of unrelated mini-apps.

### 1.2 Product principles

1.  **Offline-first:** Core functionality must work without an internet
    connection. Network access is only used when a feature explicitly
    needs it, such as downloading an AI model.
2.  **Local-first and privacy-respecting:** User content and AI
    conversations remain on the device unless the user deliberately
    exports or shares them.
3.  **Open source:** Source code, build instructions, dependencies, and
    licensing must be transparent.
4.  **User control:** Ask before adding, removing, or materially
    changing features or architecture.
5.  **Permission minimization:** Request Android permissions only when a
    user invokes a feature that needs them.
6.  **Modular design:** Modules should have clear boundaries and should
    not become tightly coupled.
7.  **No mandatory account:** The application must not require
    registration or a cloud account.
8.  **No mandatory telemetry:** No analytics or tracking by default.
9.  **Reliable data handling:** Protect user data from interrupted
    operations, invalid imports, and failed migrations.
10. **Accessible and coherent UI:** Use native Android patterns and
    Material 3.

### 1.3 Scope

The first complete release is intended to include all approved core
modules:

-   Dashboard
-   Files
-   Notes
-   Device
-   Tools
-   Local AI

These modules will be implemented incrementally through the roadmap.
"All modules in the first complete release" does not mean they must all
be developed simultaneously.

Extensions or a third-party plugin ecosystem are **not** part of the
initial release.

------------------------------------------------------------------------

## 2. Approved product decisions

  -----------------------------------------------------------------------
  Area                                Approved decision
  ----------------------------------- -----------------------------------
  Project name                        OPENOP

  Product type                        Open-source all-in-one Android
                                      toolbox

  Minimum Android version             Android 12 / API 31

  UI framework                        Kotlin + Jetpack Compose

  Design system                       Material 3 with Dynamic Color where
                                      supported

  Core logic                          Rust

  UI navigation                       Bottom navigation

  Connectivity                        Offline-first

  File access                         Shared storage, SD card, USB OTG,
                                      subject to Android/device
                                      limitations

  Local AI formats                    LiteRT-LM and GGUF

  AI engines                          LiteRT-LM backend and llama.cpp
                                      backend, behind a common interface

  AI model location                   User-selectable storage location

  Backup                              Manual export/import

  Backup contents                     Notes, settings, AI conversations,
                                      AI configurations

  Large model binaries in backup      Excluded by default

  Permissions                         Just-in-time; ask only when needed

  Initial extension system            Built-in modules only

  Distribution                        GitHub and F-Droid

  License direction                   Apache License 2.0 recommendation

  Kotlin/Rust bridge                  UniFFI

  LiteRT-LM version policy            Latest stable release, checked
                                      when implementation begins and
                                      pinned in the build

  Model download source               Hugging Face

  Model storage on SD card / USB      Allowed, in addition to
                                      user-selected internal storage,
                                      subject to Android/device
                                      limitations

  Phase 1 build decisions             D1–D9 in docs/phase-1-plan.md
                                      §10.1 (app ID io.github.<username>
                                      .openop, Apache-2.0 LICENSE,
                                      x86_64 debug-only ABI, Gradle Exec
                                      + cargo-ndk, ktlint, JNA, bindings
                                      not committed, Gradle dependency
                                      verification)
  -----------------------------------------------------------------------

### 2.1 Decisions that remain open

These points must be discussed before the relevant implementation phase.
Do not silently decide them.

-   LiteRT-LM F-Droid build compatibility (version policy is decided:
    latest stable, pinned).
-   Hugging Face download details: how users find models (search,
    curated list, or pasted link), handling of gated models that need a
    Hugging Face token, and F-Droid anti-feature implications.
-   Whether encrypted backup should be included in the first release.
-   Exact backup archive format and compatibility policy.
-   Final list of Tools utilities.
-   Detailed Dashboard widget customization.
-   Final visual branding, app icon, and naming of individual modules.
-   Any feature not explicitly approved in this document.

When a decision is required, present the options, explain their
consequences, and ask the project owner before proceeding.

------------------------------------------------------------------------

## 3. Technical architecture

### 3.1 High-level structure

OPENOP uses a native Android presentation layer and a shared Rust core.

``` text
OPENOP Android Application
|
+-- Presentation Layer (Kotlin + Jetpack Compose)
|   +-- Material 3 UI
|   +-- Screens and components
|   +-- Bottom navigation
|   +-- UI state
|
+-- Application Layer (Kotlin)
|   +-- Navigation and module coordination
|   +-- Permission requests
|   +-- Android lifecycle integration
|   +-- Platform-specific services
|
+-- Kotlin / Rust Bridge
|   +-- UniFFI
|
+-- Shared Core (Rust)
|   +-- Reusable business logic
|   +-- File processing
|   +-- Data processing
|   +-- AI orchestration and shared abstractions
|
+-- Platform and Storage Integrations
    +-- Android Storage Access Framework
    +-- SQLite / Room
    +-- LiteRT-LM
    +-- llama.cpp / GGUF
    +-- Import and export
```

### 3.2 Presentation layer

**Technology:** Kotlin, Jetpack Compose, Material 3.

Responsibilities:

-   Render all screens and components.
-   Handle user input and interaction.
-   Display loading, progress, error, and success states.
-   Adapt to different screen sizes and system settings.
-   Use Android-native APIs where appropriate.
-   Follow accessibility best practices.

The UI must not contain large amounts of business logic.

### 3.3 Application layer

Responsibilities:

-   Coordinate modules.
-   Manage navigation and application state.
-   Request permissions at the point of use.
-   Connect UI actions to core services.
-   Handle Android lifecycle events.
-   Coordinate backup and restore workflows.

### 3.4 Rust core

Rust is responsible for shared logic where it provides a meaningful
benefit.

Potential responsibilities:

-   File processing and metadata operations.
-   Hashing and data-processing utilities.
-   Reusable parsing and transformation logic.
-   AI engine abstraction and orchestration.
-   Shared validation and domain logic.

Do not move every small operation into Rust merely for consistency.
Android-specific functionality should remain in Kotlin when that is the
more reliable and maintainable choice.

### 3.5 Kotlin/Rust communication

**Decision: UniFFI.** UniFFI generates the Kotlin bindings from the
Rust interface definition, which keeps the cross-language boundary
easier to maintain than hand-written JNI.

Hand-written JNI is not used for the OPENOP core bridge. If a specific
third-party native library later requires direct JNI (for example, an
inference runtime's own bindings), raise it as a separate decision.

Keep the boundary explicit and narrow. Avoid exposing unnecessary
internal Rust implementation details to Kotlin.

### 3.6 Database and persistence

Proposed approach:

-   SQLite as the underlying database.
-   Room as the Kotlin-side database abstraction.
-   Kotlin/Room owns application database access.
-   Rust receives explicit data or service interfaces when it needs to
    process data.

Avoid having Kotlin and Rust independently manage the same database
schema or write to the same database without a deliberate coordination
design.

Database requirements:

-   Versioned schema and migrations.
-   Safe handling of interrupted updates.
-   Validation of imported data.
-   Clear ownership of user data.
-   No cloud dependency for normal operation.

### 3.7 File access and Android storage

Use Android's Storage Access Framework (SAF) for user-selected files and
directories.

Target use cases:

-   Shared storage.
-   User-selected folders.
-   SD cards.
-   USB OTG storage.

**Important Android limitation:** Access to removable storage depends on
the Android device, USB host support, filesystem, document provider, and
permissions granted by the user. Do not promise universal access to
every path or device.

The application must handle:

-   Storage disconnection.
-   Permission revocation.
-   Read-only storage.
-   Insufficient space.
-   Unsupported filesystems or providers.
-   Interrupted copy or export operations.
-   Invalid or unavailable document URIs.

Use safe temporary files and clear recovery/error messages for
operations that could otherwise leave partial data.

### 3.8 Local AI architecture

AI support must be modular and backend-independent.

``` text
AI Module
|
+-- AI Manager
|   +-- Model registry
|   +-- Model import and selection
|   +-- Conversation management
|   +-- Resource and lifecycle management
|
+-- AI Engine Interface
    |
    +-- LiteRT-LM backend
    |   +-- Compatible LiteRT-LM models
    |   +-- Android-oriented inference integration
    |
    +-- llama.cpp backend
        +-- GGUF model loading
        +-- Android ARM64 native integration
```

#### LiteRT-LM

-   Intended for compatible LiteRT-LM models.
-   Android integration is Kotlin-oriented.
-   Model compatibility must be validated rather than assumed.
-   Acceleration depends on the device, runtime, model, and supported
    delegates.
-   Do not assume that every model format can be loaded by LiteRT-LM.

#### GGUF / llama.cpp

-   Use llama.cpp for GGUF model support.
-   Build and package the native Android components for supported ABIs.
-   Start with Android ARM64 support unless additional ABIs are
    explicitly approved.
-   Validate model metadata, file access, available memory, and device
    capability before loading.

#### AI model management

Required capabilities:

-   Import a model selected by the user.
-   Display available models and their metadata where available.
-   Select the active model.
-   Show model format and storage location.
-   Check available storage before copying or importing.
-   Handle failed, cancelled, or interrupted imports.
-   Allow the user to choose a model storage location.
-   Make model deletion an explicit user action.
-   Explain compatibility limitations.
-   Do not silently download or replace models.

Model downloads from Hugging Face are approved as a source. Every
download must be started by the user and must show the source
repository, expected size, and the model's license information before
it begins. The exact download flow remains open (see section 2.1).

Models may be stored on SD cards or USB storage as well as
user-selected internal storage. Whether a given removable location
works depends on the device, document provider, and whether the
inference backend can read the file from it; check this and explain it
to the user rather than assuming it will work.

#### AI conversations

-   Local conversation history.
-   Create, rename, and delete conversations.
-   Preserve conversation configuration.
-   Stream generated responses when supported by the backend.
-   Handle cancellation and inference errors.
-   Keep model selection and conversation settings understandable.
-   Include conversations and configurations in manual backups.

Model files are excluded from backups by default because they can be
very large.

#### AI compatibility and resource management

-   Never promise that every model will run on every Android device.
-   Check memory and storage constraints where possible.
-   Handle model load failures gracefully.
-   Avoid freezing the UI during inference.
-   Allow the user to cancel generation.
-   Release native resources appropriately when models are unloaded or
    the app lifecycle requires it.
-   Document backend-specific limitations.

Each model and runtime dependency must have its own license and
redistribution review. The OPENOP application license does not
automatically apply to third-party model weights.

------------------------------------------------------------------------

## 4. Application design and navigation

### 4.1 Navigation

Use bottom navigation for the main application areas.

The exact final navigation labels and ordering can be adjusted only
after discussion and approval. The initial structure should provide
clear access to:

-   Dashboard
-   Files
-   Notes
-   Device
-   Tools
-   AI

If six destinations make the bottom bar crowded, propose a navigation
alternative and obtain approval before changing the structure.

### 4.2 Visual design

-   Material 3.
-   Dynamic Color on supported Android versions.
-   Light and dark themes.
-   Consistent spacing, typography, and component behavior.
-   Responsive layouts.
-   Clear empty states.
-   Clear loading and error states.
-   Accessible contrast and touch targets.
-   Avoid unnecessary visual clutter.

### 4.3 User experience rules

-   Ask for permissions only when a feature needs them.
-   Explain why a permission is required.
-   Never make a permission request appear unrelated to the user's
    action.
-   Confirm destructive actions.
-   Show progress for long-running operations.
-   Provide cancellation where safe.
-   Preserve user work after recoverable errors.
-   Do not add a feature, remove a feature, or change a core behavior
    without asking the project owner.

------------------------------------------------------------------------

## 5. Full feature specification

The following is the planned feature scope. Items marked **Core** are
part of the intended first complete release. Optional or advanced
features must be reviewed before being added.

## 5.1 Dashboard

**Purpose:** Provide a useful overview of OPENOP and quick access to its
modules.

### Core features

-   Overview screen.
-   Shortcuts to main modules.
-   Overview of relevant device information.
-   Storage overview where Android permissions and APIs allow it.
-   Clear entry points to Files, Notes, Device, Tools, and AI.
-   Useful empty states for modules that have not yet been configured.

### Proposed enhancements --- approval required

-   Customizable dashboard widgets.
-   Reorderable module shortcuts.
-   Optional quick actions.
-   User-selected dashboard layout.

Do not add notifications, background monitoring, or persistent services
without a separate discussion.

## 5.2 Files

**Purpose:** Provide a practical Android file manager.

### Core features

-   Browse user-accessible storage locations.
-   Browse user-selected folders.
-   Navigate directories.
-   View file and folder names, types, sizes, and relevant metadata.
-   Open files through compatible Android applications.
-   Copy files.
-   Move files.
-   Rename files and folders.
-   Delete files and folders with confirmation.
-   Create folders.
-   Search accessible files.
-   Sort and filter file listings.
-   Multi-select files.
-   Show operation progress.
-   Handle errors and interrupted operations.
-   Work with supported shared storage, SD cards, and USB OTG locations
    through Android's permitted access mechanisms.

### Advanced features --- confirm before implementation

-   Archive creation and extraction.
-   Batch file operations.
-   File hashing and checksum verification.
-   Storage usage analysis.
-   Duplicate-file discovery.
-   More advanced file filtering.

### Files module constraints

-   Respect Android scoped-storage rules.
-   Never claim unrestricted access to Android's private app
    directories.
-   Handle unavailable storage providers.
-   Avoid destructive operations without confirmation.
-   Make partial-operation failures visible.

## 5.3 Notes

**Purpose:** Provide a local note-taking system.

### Core features

-   Create notes.
-   Edit notes.
-   Delete notes with confirmation.
-   Automatic saving.
-   Search notes.
-   Organize notes.
-   Markdown support.
-   Rich-text editing support.
-   Local persistence.
-   Include notes in manual backups.

### Proposed organization features

-   Folders.
-   Tags.
-   Sorting and filtering.
-   Pinned notes.

Confirm the exact organization model before implementing it if it
affects the data model or UI.

### Data safety

-   Avoid losing edits when navigating away.
-   Handle interrupted writes.
-   Use versioned storage structures.
-   Validate notes during backup restore.

## 5.4 Device

**Purpose:** Show useful information about the Android device.

### Core features

Where Android APIs permit, display:

-   Device manufacturer and model.
-   Android version.
-   API level.
-   CPU information.
-   GPU information where available.
-   RAM information where available.
-   Storage capacity and available space where accessible.
-   Battery information exposed by Android.
-   Screen and display information.
-   Relevant hardware and software capabilities.

### Device information limitations

Android restricts access to some system and hardware details. Show only
information that can be obtained reliably through supported APIs.

Do not imply that OPENOP can inspect every process, system partition,
sensor, or hardware component.

## 5.5 Tools

**Purpose:** Offer practical utilities in one place.

### Initial direction

The Tools module is part of the first complete release, but its exact
utility list is not yet finalized.

### Candidate utilities --- approval required

-   Calculator.
-   Unit converter.
-   Text utilities.
-   QR code generator.
-   File hash calculator.
-   Checksum verification.
-   Other small offline utilities proposed by the project owner.

Do not silently expand the module. Present proposed utilities with
implementation cost, dependencies, permissions, and offline behavior
before adding them to the approved scope.

## 5.6 Local AI

**Purpose:** Run compatible AI models locally on Android.

### Core features

-   Local AI interface.
-   LiteRT-LM model support.
-   GGUF model support through llama.cpp.
-   User-selected model storage location.
-   User-imported models.
-   Model registry.
-   Model selection.
-   Local conversation history.
-   Conversation management.
-   Model and conversation configuration.
-   Streaming output where supported.
-   Cancellation where supported.
-   Error handling and model compatibility messages.
-   Manual backup of AI conversations and configurations.

### Model import requirements

-   Use Android's file picker or SAF.
-   Validate file access.
-   Check available storage.
-   Show progress for large imports.
-   Support cancellation.
-   Handle interrupted imports.
-   Do not overwrite an existing model without explicit confirmation.
-   Keep model binaries outside the normal backup by default.

### Future possibilities --- not approved for initial scope

Hugging Face is approved as the download source (section 2.1). The
items below remain unapproved until the download flow is decided.

-   Built-in model catalogue.
-   One-tap model downloads.
-   Model recommendations based on device capability.
-   Additional inference backends.
-   Advanced generation controls.

These require separate approval and a dependency, licensing, storage,
and compatibility review.

------------------------------------------------------------------------

## 6. Backup and restore

### 6.1 Backup scope

Manual backup must include:

-   Notes.
-   Application settings.
-   AI conversations.
-   AI configurations.

Large model binaries are excluded by default.

### 6.2 Backup behavior

-   User explicitly starts an export.
-   User chooses the destination.
-   Show progress.
-   Handle insufficient storage and write errors.
-   Avoid silently replacing an existing backup.
-   Include a format/schema version.
-   Validate backup contents before completing an import.

### 6.3 Restore behavior

-   User explicitly selects a backup.
-   Validate archive structure and schema version.
-   Explain incompatible or unsupported data.
-   Avoid partial destructive restores where possible.
-   Confirm before replacing or merging existing data.
-   Preserve the current data if validation fails.

### 6.4 Not yet decided

-   Whether backups should support encryption in the first release.
-   Whether restore should merge or replace by default.
-   Exact archive format.
-   Backup compatibility and migration policy.

Ask the project owner before finalizing these decisions.

------------------------------------------------------------------------

## 7. Security, privacy, and permissions

### 7.1 Privacy requirements

-   No mandatory account.
-   No mandatory cloud service.
-   No analytics or telemetry by default.
-   No background data upload.
-   Local storage for notes and conversations.
-   Network access only for explicitly requested online operations.
-   Clear disclosure of any optional network-dependent feature.

### 7.2 Permission strategy

Use just-in-time permissions.

Examples:

-   Request access to a folder when the user chooses to browse it.
-   Request file access through SAF when importing or exporting.
-   Do not request broad access at first launch merely because the app
    contains a file manager.

### 7.3 Security requirements

-   Validate imported files and backup archives.
-   Handle malformed model files safely.
-   Avoid unsafe path handling.
-   Avoid logging private note contents or conversation contents.
-   Do not expose sensitive data in crash logs.
-   Use safe native-library boundaries.
-   Review third-party dependencies and their licenses.
-   Keep destructive actions explicit and reversible where practical.

------------------------------------------------------------------------

## 8. Dependencies and compatibility policy

The final dependency versions must be checked against current stable
releases when implementation begins. Do not invent or hardcode version
numbers based on stale documentation.

### Main technology areas

  -----------------------------------------------------------------------
  Area                                Planned technology
  ----------------------------------- -----------------------------------
  Android UI                          Kotlin

  UI framework                        Jetpack Compose

  Design                              Material 3

  Shared core                         Rust

  Kotlin/Rust bridge                  UniFFI

  Database                            SQLite / Room

  File access                         Android Storage Access Framework

  LiteRT-LM inference                 LiteRT-LM Android integration

  GGUF inference                      llama.cpp

  Android native builds               Android NDK

  Build system                        Gradle + Cargo

  Source control                      Git

  CI                                  GitHub Actions or another approved
                                      open-source-compatible CI setup
  -----------------------------------------------------------------------

### Dependency review requirements

Before adding a dependency, document:

-   What feature requires it.
-   Whether an existing dependency can already provide the capability.
-   License.
-   Maintenance status.
-   Android compatibility.
-   Native binary requirements.
-   Build and reproducibility impact.
-   F-Droid compatibility.
-   Privacy and network behavior.

Avoid unnecessary dependencies and avoid proprietary services in the
F-Droid build.

------------------------------------------------------------------------

## 9. Licensing and distribution

### 9.1 Recommended application license: Apache License 2.0

Apache 2.0 is the current recommendation because it:

-   Is an established open-source license.
-   Includes an express patent grant.
-   Allows reuse and redistribution under its terms.
-   Is compatible with GPLv3.
-   Supports a broad ecosystem of contributors and downstream projects.

This is a recommendation recorded in the architecture, not a substitute
for adding the actual license files and notices during implementation.

If the project owner later prefers strong copyleft, GPLv3 should be
compared before the license is finalized.

### 9.2 Third-party licenses

The project license does not automatically cover:

-   Third-party libraries.
-   Native inference runtimes.
-   AI model weights.
-   Downloaded model assets.
-   Fonts, icons, and other assets.

Track and review each component separately. Preserve required notices
and attribution.

### 9.3 GitHub

Planned distribution:

-   Public source repository.
-   Build instructions.
-   Issue tracking.
-   Release notes.
-   Tagged releases.
-   APK artifacts where appropriate.
-   Clear installation and compatibility documentation.

### 9.4 F-Droid

F-Droid compatibility must be considered from the beginning.

Requirements to investigate and satisfy:

-   FLOSS-compatible app and dependencies.
-   Transparent source builds.
-   No proprietary tracking or advertising SDKs.
-   No mandatory Google Play Services dependency.
-   Build reproducibility and documented build process.
-   Appropriate licensing and attribution.
-   Clear handling of any dependency that cannot be included in the main
    F-Droid build.

**Important:** LiteRT-LM and all native dependencies must undergo a
specific F-Droid policy and build audit. Do not assume compatibility. If
a dependency prevents inclusion in the main repository, discuss a
compliant alternative or a clearly separated build configuration before
changing features.

------------------------------------------------------------------------

## 10. Development roadmap

The roadmap is incremental. Each phase must produce a reviewable result
before the next phase begins.

No phase should silently change the approved architecture or product
scope.

## Phase 0 --- Product and architecture approval

**Status: Approved**

Objectives:

-   Define OPENOP's purpose and principles.
-   Establish the first-release module list.
-   Approve Android compatibility.
-   Approve the Kotlin/Compose and Rust architecture.
-   Define offline-first and privacy expectations.
-   Establish storage, AI, backup, licensing, and distribution
    direction.

Deliverables:

-   Project vision.
-   Approved architecture.
-   Feature scope.
-   Roadmap.
-   List of unresolved decisions.

Exit criteria:

-   Project owner approves the architecture.
-   No implementation begins without separate authorization.

## Phase 1 --- Repository and development foundation

**Status: Implemented --- awaiting owner review (see docs/phase-1-plan.md §13)**

Objectives:

-   Create the repository structure.
-   Establish Android and Rust build integration.
-   Configure the Android NDK.
-   Establish development and release build variants.
-   Configure formatting, linting, and tests.
-   Establish a repeatable local build.

Planned work:

-   Create the Android application project.
-   Create the Rust core crate.
-   Configure Gradle and Cargo integration.
-   Configure supported Android ABI targets.
-   Establish the Kotlin/Rust bridge using UniFFI.
-   Add basic logging and error conventions.
-   Add Git ignore rules and project documentation.
-   Establish CI build checks.

Deliverables:

-   Buildable empty Android application.
-   Buildable Rust library for the selected target.
-   Working Kotlin/Rust boundary demonstration.
-   Documented development setup.
-   Initial automated checks.

Exit criteria:

-   A clean checkout can be built using documented instructions.
-   Android application launches on a supported Android 12+ device or
    emulator.
-   Kotlin can call a small Rust function and handle its result.
-   CI can run the agreed initial checks.

## Phase 2 --- Application shell and design system

**Status: Planned --- not started**

Objectives:

-   Establish the visual identity and navigation foundation.
-   Create reusable UI components.
-   Make the application shell responsive and accessible.

Planned work:

-   Material 3 setup.
-   Dynamic Color support where available.
-   Light and dark themes.
-   Bottom navigation.
-   Main module destinations.
-   Reusable buttons, cards, dialogs, lists, and input components.
-   Loading, empty, error, and confirmation states.
-   Basic accessibility checks.

Deliverables:

-   Navigable application shell.
-   Reusable design components.
-   Placeholder screens for approved modules.
-   Theme and navigation documentation.

Exit criteria:

-   All approved destinations can be opened.
-   Navigation behaves consistently.
-   Themes render correctly.
-   No module functionality is falsely presented as complete.

## Phase 3 --- Data, settings, and application infrastructure

**Status: Planned --- not started**

Objectives:

-   Establish reliable persistence and common services.

Planned work:

-   SQLite/Room setup.
-   Database schema and migration strategy.
-   Application settings.
-   Preference storage.
-   Permission request coordinator.
-   Shared error handling.
-   Logging conventions.
-   Backup architecture design.
-   Common progress and cancellation patterns.

Deliverables:

-   Working database layer.
-   Versioned schema.
-   Settings persistence.
-   Permission handling foundation.
-   Common service and error patterns.

Exit criteria:

-   Data survives application restarts.
-   Database migrations are tested.
-   Permission requests happen only when needed.
-   Errors are understandable and do not silently discard user data.

## Phase 4 --- Dashboard and Device modules

**Status: Planned --- not started**

Objectives:

-   Deliver the first useful overview and device information modules.

Dashboard work:

-   Main overview.
-   Module shortcuts.
-   Basic device summary.
-   Storage overview where supported.

Device work:

-   Manufacturer and model.
-   Android version and API level.
-   CPU and GPU information where available.
-   RAM and storage information where available.
-   Battery and display information exposed by supported APIs.

Deliverables:

-   Functional Dashboard.
-   Functional Device information screen.
-   Documented API limitations.

Exit criteria:

-   Displayed values are sourced from supported APIs.
-   Unsupported values are clearly identified.
-   Screens handle unavailable data gracefully.

## Phase 5 --- Files module

**Status: Planned --- not started**

Objectives:

-   Deliver the core file manager with Android-compliant storage access.

Planned work:

-   SAF integration.
-   User-selected folder access.
-   Directory browsing.
-   File metadata.
-   Create folders.
-   Copy, move, rename, and delete.
-   Multi-selection.
-   Search and sorting.
-   Open files through Android.
-   Progress and cancellation.
-   SD card and USB OTG testing on supported hardware.

Deliverables:

-   Functional Files module.
-   Storage-provider error handling.
-   Documentation of storage limitations.

Exit criteria:

-   Operations work within granted access.
-   Permission revocation is handled.
-   Disconnected storage is handled.
-   Interrupted operations do not silently corrupt data.
-   Destructive actions require confirmation.

## Phase 6 --- Notes module

**Status: Planned --- not started**

Objectives:

-   Deliver reliable local note-taking.

Planned work:

-   Note creation and editing.
-   Automatic saving.
-   Markdown support.
-   Rich-text editing support.
-   Search.
-   Local persistence.
-   Agreed organization features after approval.
-   Backup integration.

Deliverables:

-   Functional Notes module.
-   Note data model.
-   Tested persistence and backup behavior.

Exit criteria:

-   Notes survive restarts.
-   Editing does not silently lose work.
-   Search returns relevant notes.
-   Backup and restore preserve note contents.

## Phase 7 --- Local AI module

**Status: Planned --- not started**

Objectives:

-   Integrate both approved local model formats through a common AI
    architecture.

Planned work:

1.  Finalize the AI engine interface.
2.  Verify LiteRT-LM Android integration and licensing.
3.  Verify llama.cpp Android ARM64 build and licensing.
4.  Implement the LiteRT-LM backend.
5.  Implement the GGUF backend.
6.  Build model import and registry.
7.  Implement user-selectable model storage.
8.  Add model compatibility and resource checks.
9.  Add conversation storage and management.
10. Add generation streaming and cancellation where supported.
11. Integrate AI conversations and configurations with backup.

Deliverables:

-   AI manager.
-   AI engine abstraction.
-   LiteRT-LM backend.
-   GGUF backend.
-   Model import and selection.
-   Local conversations.
-   Resource and error handling documentation.

Exit criteria:

-   Compatible test models load through their respective backends.
-   Unsupported models fail gracefully with useful explanations.
-   Model import handles cancellation and insufficient storage.
-   Inference does not freeze the UI.
-   Conversations persist locally.
-   Model binaries remain excluded from normal backups by default.
-   Dependency and F-Droid compatibility review is documented.

## Phase 8 --- Tools module

**Status: Planned --- not started**

Objectives:

-   Implement the final approved collection of offline utilities.

Planned work:

-   Confirm the exact initial utility list with the project owner.
-   Implement each approved utility.
-   Reuse common UI components.
-   Keep utilities offline where possible.
-   Test edge cases and invalid inputs.

Deliverables:

-   Functional Tools module.
-   Documentation for each utility.

Exit criteria:

-   Every included tool has tests or documented manual verification.
-   No unapproved utilities are added.
-   Utilities work offline unless explicitly documented otherwise.

## Phase 9 --- Integration, quality, and hardening

**Status: Planned --- not started**

Objectives:

-   Verify that the modules work together as one application.

Planned work:

-   End-to-end testing.
-   Database migration testing.
-   Backup and restore testing.
-   Storage interruption testing.
-   Permission revocation testing.
-   AI model lifecycle testing.
-   Performance and memory profiling.
-   Battery impact checks.
-   Accessibility review.
-   Security review.
-   Dependency and license audit.
-   F-Droid build compatibility review.
-   Documentation review.

Deliverables:

-   Test report.
-   Known limitations list.
-   Dependency and license inventory.
-   Release candidate checklist.

Exit criteria:

-   No known critical data-loss issue.
-   Core flows pass on supported Android versions and devices.
-   Errors are visible and recoverable where possible.
-   Licensing and distribution blockers are resolved or explicitly
    documented.

## Phase 10 --- Release preparation and distribution

**Status: Planned --- not started**

Objectives:

-   Prepare the first complete OPENOP release.

Planned work:

-   Finalize application metadata.
-   Finalize license and notices.
-   Prepare source and build documentation.
-   Prepare release notes.
-   Build and test release artifacts.
-   Verify GitHub release process.
-   Prepare F-Droid metadata and build submission.
-   Review reproducibility requirements.
-   Publish known limitations and compatibility information.

Deliverables:

-   Version 1.0 release candidate.
-   GitHub source release.
-   Installable release artifact where appropriate.
-   F-Droid submission package/metadata.
-   User documentation.
-   Contributor documentation.

Exit criteria:

-   Release builds are reproducible to the extent required by the chosen
    distribution.
-   Installation and upgrade paths are tested.
-   License and third-party notices are present.
-   Distribution-specific requirements are met.
-   Project owner explicitly approves publication.

------------------------------------------------------------------------

## 11. Testing strategy

Testing must be introduced from the beginning rather than postponed
until the release phase.

### 11.1 Unit tests

Use for:

-   Rust core logic.
-   File metadata and processing helpers.
-   Data validation.
-   AI configuration handling.
-   Note and settings logic.
-   Tool calculations and transformations.

### 11.2 Android tests

Use for:

-   Compose UI behavior.
-   Navigation.
-   Permission flows.
-   Storage Access Framework integration.
-   Database persistence.
-   Backup and restore.
-   Android lifecycle handling.

### 11.3 Device testing

Test on:

-   Android 12 / API 31 minimum.
-   A recent Android version.
-   Different screen sizes where practical.
-   Devices with different memory capacities.
-   Devices with and without removable storage.
-   USB OTG hardware when available.
-   ARM64 devices for native AI inference.

### 11.4 Failure testing

Explicitly test:

-   App process termination during an operation.
-   Storage disconnection.
-   Permission revocation.
-   Insufficient storage.
-   Corrupted backup.
-   Unsupported model.
-   Model loading failure.
-   Inference cancellation.
-   Database migration failure.

------------------------------------------------------------------------

## 12. Performance and reliability expectations

-   Keep UI interactions responsive.
-   Run expensive processing away from the main UI thread.
-   Show progress for long-running work.
-   Support cancellation when safe.
-   Avoid loading entire large files into memory unnecessarily.
-   Avoid unnecessary background services.
-   Release native resources correctly.
-   Handle low-memory conditions gracefully.
-   Avoid silently retrying expensive AI operations.
-   Do not claim performance targets until measured on actual devices.

Performance targets should be proposed and approved after an initial
working build provides a baseline.

------------------------------------------------------------------------

## 13. Documentation requirements

The repository should eventually contain:

-   `README.md` --- project overview, features, installation, and build
    instructions.
-   `CHATGPT.md` --- this full project context and roadmap.
-   `LICENSE` --- selected application license.
-   `NOTICE` --- third-party notices where required.
-   `CONTRIBUTING.md` --- contribution and development workflow.
-   `SECURITY.md` --- responsible security reporting guidance.
-   `CHANGELOG.md` --- release history.
-   Architecture documentation.
-   Module-specific documentation.
-   Build and F-Droid documentation.

Documentation must reflect the actual implementation. Do not claim a
feature exists until it is implemented and tested.

------------------------------------------------------------------------

## 14. Project-owner approval and change-control rules

The project owner wants to remain involved in product and architecture
decisions.

Therefore, the AI assistant or contributor must follow these rules:

1.  **Ask before adding a feature.** Explain the feature, benefit, cost,
    dependencies, and trade-offs.
2.  **Ask before removing a feature.** Explain why removal is being
    proposed and what would be lost.
3.  **Ask before changing a feature's behavior.** Explain the current
    behavior and proposed replacement.
4.  **Ask before changing architecture.** Explain compatibility,
    maintenance, performance, and distribution consequences.
5.  **Ask before adding a major dependency.** Include its license and
    F-Droid implications.
6.  **Do not silently reduce scope.** If a feature is difficult, propose
    options rather than quietly omitting it.
7.  **Do not silently expand scope.** Keep unapproved ideas in a
    proposal list.
8.  **Work in reviewable phases.** Report completed work, tests,
    limitations, and next steps.
9.  **Do not claim tests passed unless they were actually run.**
10. **Do not claim a release is ready until the release criteria are
    met.**

### Suggested decision format

When a decision is needed, present:

-   **Decision required**
-   **Current approved behavior**
-   **Proposed change**
-   **Reason**
-   **Advantages**
-   **Disadvantages and risks**
-   **Dependencies and licensing impact**
-   **Effect on Android/F-Droid compatibility**
-   **Recommendation**
-   **Approval question**

Wait for the project owner's answer before implementing a scope or
architecture change.

------------------------------------------------------------------------

## 15. Instructions for AI coding assistants

When an AI coding assistant reads this file:

-   Treat this document as the current project specification.
-   Do not assume that planned features are already implemented.
-   Do not start implementation unless the user explicitly authorizes
    it.
-   Work only on the phase the user has authorized.
-   Before coding, inspect the existing repository and report its actual
    state.
-   Do not overwrite or remove existing work without permission.
-   Do not invent test results, build results, files, or dependencies.
-   Check current official documentation for version-sensitive APIs and
    dependencies.
-   Keep the project buildable at the end of each implementation
    increment.
-   Update documentation when approved implementation changes are made.
-   Ask the project owner before changing scope, features, architecture,
    dependencies, or distribution plans.
-   Prefer small, reviewable changes over large unreviewable rewrites.
-   Explain limitations honestly, especially Android storage
    restrictions and local AI model compatibility.

### Implementation authorization rule

**Approval of this architecture is not blanket authorization to write
code.**

The architecture has been approved. The next step is to prepare the
detailed Phase 1 implementation plan. Actual repository creation or code
changes require a separate explicit instruction from the project owner.

------------------------------------------------------------------------

## 16. Current project status

  -----------------------------------------------------------------------
  Item                                Status
  ----------------------------------- -----------------------------------
  Product vision                      Defined

  Core modules                        Defined

  Android minimum version             Approved

  Kotlin + Compose UI                 Approved

  Rust core                           Approved

  Offline-first direction             Approved

  Shared storage / SD / USB OTG       Approved, subject to Android
  direction                           limitations

  LiteRT-LM + GGUF direction          Approved

  User-selectable model storage       Approved

  Manual backup scope                 Approved

  Kotlin/Rust bridge (UniFFI)         Approved

  LiteRT-LM: latest stable, pinned    Approved; F-Droid audit pending

  Hugging Face model downloads        Approved as source; flow pending

  Models on SD card / USB             Approved, subject to Android
                                      limitations

  Apache 2.0                          Recommended; final license files
                                      not yet created

  GitHub + F-Droid                    Approved targets; compatibility
                                      audits pending

  Architecture                        Approved

  Phase 1 (foundation)                Implemented; awaiting owner
                                      review. CI not yet run on GitHub;
                                      API 31 device check pending
                                      (docs/phase-1-plan.md §13)

  Implementation                      Phase 1 done; Phase 2 not
                                      authorized

  Release                             Not started
  -----------------------------------------------------------------------

## 17. Immediate next step

Prepare a detailed **Phase 1 implementation specification** containing:

-   Proposed repository directory tree.
-   Android project and Rust crate structure.
-   Current stable toolchain versions to verify.
-   Gradle, Cargo, and Android NDK integration plan.
-   UniFFI integration plan (UniFFI is decided).
-   Supported Android ABI plan.
-   CI workflow.
-   Initial unit and integration tests.
-   Build and acceptance criteria.

Do not create the repository or write implementation code until the
project owner separately authorizes implementation.

------------------------------------------------------------------------

**End of OPENOP project specification.**
