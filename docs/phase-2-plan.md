# OPENOP — Phase 2 Implementation Plan

> **Phase:** 2 — Application shell and design system
> **Status:** Implemented (2.1–2.5 and the 2.6 docs); CI green. The phone
> check (visuals, insets, TalkBack) is still pending (§10, §15).
> **Date:** 2026-11-19 (rev. 1: 2026-11-16)
> **Scope source:** `CLAUDE.md` §4 and §10 (Phase 2)
> **Builds on:** Phase 1 (complete, CI green)

This document is a plan only. It does not authorize code changes.
Section 3 lists the decisions the project owner needs to make before
Phase 2 can start. Several of them (navigation structure, labels, app
icon) are ones CLAUDE.md says must not be decided silently.

**Rev. 2** adds the results of an architecture review and an API check
against current official documentation (2026-11-18). The changes are
listed in §14.

------------------------------------------------------------------------

## 1. Goal

When Phase 2 is done, OPENOP is a navigable app shell:

- every approved destination (Dashboard, Files, Notes, Device, Tools,
  AI) can be opened;
- navigation and the back button behave consistently, keep their state,
  and survive rotation and process death;
- light and dark themes with Dynamic Color render correctly, with no
  wrong-colour flash at launch;
- reusable components (states, dialogs, buttons, cards, lists, inputs)
  exist and are documented;
- layouts adapt to phones, foldables and tablets;
- accessibility checks run in the tests.

**No module functionality.** Every destination shows an honest
placeholder ("Planned for Phase N") and nothing that looks like a
working feature (CLAUDE.md Phase 2 exit criterion).

### Out of scope for Phase 2

- Settings, persistence, Room, the permission coordinator, and the
  common progress/cancellation pattern (all Phase 3).
- Any real Dashboard, Files, Notes, Device, Tools or AI feature
  (Phases 4–8).
- Final branding (still open), unless decided in P2-D7.
- Translations. All text goes in string resources, English only.
- Screenshot testing (possible later proposal; see §11).

------------------------------------------------------------------------

## 2. Current state (from Phase 1)

- `:app` with a single placeholder screen showing the Rust core
  version.
- `:core-bridge` (UniFFI) and `rust/`.
- Compose BOM 2026.09.00 (Compose UI 1.12.1, Material 3 1.4.0).
- Dynamic Color is always available because minSdk is 31.
- The manifest uses `@android:style/Theme.DeviceDefault.NoActionBar`,
  which flashes a light window before Compose draws in dark mode.
  Fixed in 2.2.
- CI: Rust, Android, emulator tests on API 31 and 37.

**Versions to re-check at implementation start.** On 2026-11-18 the
release notes announced Navigation 3 1.2.2, Material 3 1.5.0 and
Compose 1.13.0, but they weren't on Google Maven yet. The plan uses
the versions available today and moves to newer stable ones if they
are published before 2.1 starts.

------------------------------------------------------------------------

## 3. Decisions needed before implementation

### P2-D1 — Navigation structure for six destinations ⚠️ main decision

CLAUDE.md asks for bottom navigation with six destinations, and says
to propose an alternative if six is crowded. Android's guidance is
navigation bars for **"three to five destinations of equal
importance"** (developer.android.com, Compose navigation bar). On a
typical phone (360–412 dp wide), six items leave about 60–68 dp each.
The labels "Dashboard" and "Notes" then get truncated or need smaller
text, and it gets worse at large font sizes.

| Option | Phone layout | Pros | Cons |
|---|---|---|---|
| **A. Six-item bar** | Dashboard · Files · Notes · Device · Tools · AI | Matches the spec literally; every area one tap away | Outside guidance; cramped labels; poor at large font sizes (accessibility) |
| **B. Five items with "More"** (recommended) | Dashboard · Files · Notes · AI · **More** → Device, Tools | Within guidance; readable labels; the Dashboard also links to all six | Device and Tools take two taps (or one from the Dashboard) |
| **C. Four items with "More"** | Dashboard · Files · Notes · **More** → Device, Tools, AI | Roomiest | AI is a core module; hiding it seems wrong |
| **D. Navigation drawer** | Hamburger menu | Unlimited items | Not bottom navigation; against the approved decision |

**Wide screens** (tablets, unfolded foldables, landscape) switch to a
**navigation rail that shows all six destinations directly**, with no
"More". This is possible with the chosen library (verified, see P2-D3).

**Recommendation: B.** Which two go under "More" is your call.

### P2-D2 — Navigation library

| Option | Version (checked 2026-11-18) | Notes |
|---|---|---|
| **Navigation 3** (`androidx.navigation3`) (recommended) | 1.2.1 stable | Built for Compose. **The app owns the back stack**: `NavDisplay` takes any snapshot-state `List<T>` of keys. Official recipes cover multiple per-tab back stacks. Predictive back is built in |
| Navigation Compose (`androidx.navigation:navigation-compose`) | 2.10.2 stable | Mature; route-based NavController; per-tab back stacks are more awkward |

Both are Apache-2.0 and FLOSS.

**Recommendation: Navigation 3, with an app-owned back stack.**
Navigation 3's own `rememberNavBackStack` requires every key to be
`@Serializable`, which would add kotlinx.serialization (plugin and
runtime). Instead, the back stack is a plain Kotlin state object saved
with a small custom saver. Navigation 3 officially supports this; its
documentation and the `basicparcelable` recipe use
`rememberSaveable { mutableStateListOf(...) }`. **No new serialization
dependency.**

### P2-D3 — Adaptive navigation (bar / rail)

Use `androidx.compose.material3:material3-adaptive-navigation-suite`
(1.4.0, managed by the Compose BOM). Its `NavigationSuiteScaffold`
picks the layout from the window size:

- compact width → short navigation bar;
- otherwise → wide navigation rail.

The item list is ordinary Compose code, so the bar can show the five
items of option B while the rail shows all six. This was confirmed
from the 1.4.0 source.

**Recommendation: yes.**

### P2-D4 — Module structure

| Option | Structure | Pros | Cons |
|---|---|---|---|
| **Feature modules** (recommended) | `:core-navigation` (pure Kotlin), `:core-ui` (theme + components), `:feature:dashboard`, `:feature:files`, `:feature:notes`, `:feature:device`, `:feature:tools`, `:feature:ai`, plus `build-logic/` convention plugins | Gradle enforces boundaries (CLAUDE.md §1.2 "modular design"); features build and test separately; sets the pattern for Phases 4–8 | More Gradle files (kept small by convention plugins); slightly slower first build |
| Packages in `:app` | `app/.../feature/<name>` | Fewer files | Boundaries only by convention; harder to split later |

**Dependency rules:**

```text
:app ──► :feature:* ──► :core-ui
  │           └───────► :core-navigation   (pure Kotlin, no Android)
  └──► :core-bridge
```

- Features never depend on each other or on `:core-bridge`.
- `:app` is the only module that knows every feature and wires them
  together.
- The Dashboard's shortcuts receive an `onOpen(destination)` callback.
- The Rust core footer receives a `CoreStatus` value from `:app`.

**Recommendation: feature modules.** `build-logic/` replaces the
Android/Kotlin/lint/ktlint settings duplicated in `app/` and
`core-bridge/`. The Rust build tasks move into an `openop.rust-bridge`
convention plugin. This is an architecture change, which is why it's
listed here.

### P2-D5 — Icons

| Option | Notes |
|---|---|
| **Vendored Material Symbols as vector drawables** (recommended) | Apache-2.0. Only the icons screens actually use are copied in; lint fails on unused resources. This is what the Compose docs now recommend |
| `material-icons-core` / `-extended` | **No longer maintained:** frozen at 1.7.8 (Feb 2025). Material 3 1.4.0 dropped its dependency on them |

**Recommendation: vendored Material Symbols.** Attribution goes in a
new `NOTICE` file. It isn't required by the licence, but CLAUDE.md
§9.2 asks for third-party assets to be tracked.

### P2-D6 — Destination labels (open "naming of individual modules")

Proposed labels, from the spec: **Dashboard, Files, Notes, Device,
Tools, AI**, plus **More** if P2-D1 = B or C. Alternative: "Home"
instead of "Dashboard".

**Recommendation:** keep the spec names. They live in string resources,
so changing them later is easy.

### P2-D7 — App icon and brand colour (open "final visual branding")

Dynamic Color takes the app's colours from the wallpaper, so a brand
colour is only needed for the launcher icon, screenshots and previews.

| Option | Notes |
|---|---|
| **Keep deferring** (recommended unless you have a design) | Launcher keeps the generic Android icon; lint check stays disabled |
| Neutral placeholder icon | Simple geometric adaptive icon, clearly marked temporary |
| You provide or choose an icon | Adaptive icon (foreground + background + monochrome for themed icons) |

### P2-D8 — Accessibility checks in tests

Add `androidx.compose.ui:ui-test-junit4-accessibility` (test-only,
from the Compose BOM). It pulls in Google's Accessibility Test
Framework 4.1.1 (Apache-2.0). `composeTestRule.enableAccessibilityChecks()`
then checks every screen before each test action. The checks cover
touch target size, text and image contrast, missing labels, duplicate
descriptions, traversal order and text size.

**Limitation found:** these checks need **API 34 or newer**. They run
on the API 37 emulator and on your phone (API 36), and are skipped on
the API 31 emulator. The other tests still run on API 31.

**Recommendation: yes.**

### P2-D9 — Where the Rust core demo goes

| Option | Notes |
|---|---|
| **Small footer on the Dashboard placeholder** (recommended) | Keeps the end-to-end bridge check visible and tested; replaced by the real Dashboard in Phase 4 |
| An "About" screen | Needs a new entry point; better with Settings in Phase 3 |
| Remove it | Instrumented tests still cover the bridge |

### P2-D10 — Debug-only component catalogue

A screen listing every component in light and dark, in **debug builds
only**. It lets you review the design on your phone without Android
Studio, where `@Preview` would otherwise be the only way.

**Recommendation: yes**, as the last item of 2.3; cheap to drop if
time is short.

### P2-D11 — Component set (new in rev. 2)

The architecture review pointed out that only a few components have a
user in Phase 2, since the screens are placeholders. CLAUDE.md
Phase 2, however, explicitly lists "buttons, cards, dialogs, lists,
and input components" and "loading, empty, error, and confirmation
states".

| Option | Phase 2 delivers |
|---|---|
| **Spec set** (recommended) | Everything CLAUDE.md Phase 2 lists: `ScreenScaffold`, `EmptyState`, `LoadingState`, `ErrorState`, `ConfirmDialog` / `DestructiveConfirmDialog`, `PrimaryButton` / `SecondaryButton` / `DestructiveButton`, `InfoCard`, `ListRow` / `SectionHeader`, `LabeledTextField` |
| Minimal set | Only what placeholders use (`ScreenScaffold`, `EmptyState`, `InfoCard`, `SectionHeader`, `ConfirmDialog`); the rest arrive with the phase that needs them. **This reduces scope**, so it needs your approval |

**Recommendation: spec set.** `ProgressRow` from rev. 1 moves to
Phase 3, which already plans the common progress and cancellation
pattern. That aligns with the spec rather than reducing it.

### 3.1 Approved answers (2026-11-19)

The project owner accepted every recommendation:

- **P2-D1:** Option B. The bottom bar is Dashboard · Files · Notes ·
  AI · More, with Device and Tools under More. The navigation rail on
  wide windows shows all six destinations.
- **P2-D2:** Navigation 3, with an app-owned back stack and a custom
  saver. No kotlinx.serialization.
- **P2-D3:** `NavigationSuiteScaffold` (material3-adaptive-navigation-suite).
- **P2-D4:** Feature modules: `:core-navigation`, `:core-ui`,
  `:feature:*`, and `build-logic/` convention plugins, including
  `openop.rust-bridge`.
- **P2-D5:** Vendored Material Symbols vector drawables, only those
  used, listed in `NOTICE`.
- **P2-D6:** Labels Dashboard, Files, Notes, Device, Tools, AI, More.
- **P2-D7:** App icon and brand colour stay deferred.
- **P2-D8:** Accessibility checks in UI tests (API 34+).
- **P2-D9:** Rust core footer on the Dashboard placeholder.
- **P2-D10:** Debug-only component catalogue.
- **P2-D11:** The full spec component set. `ProgressRow` moves to
  Phase 3.

------------------------------------------------------------------------

## 4. Navigation behaviour (option B)

### 4.1 Model

Five top-level **tabs**: Dashboard, Files, Notes, AI, More. Device and
Tools are destinations **inside More**. Each tab has its own back stack:

```text
Dashboard: [Dashboard]
Files:     [Files]
Notes:     [Notes]
AI:        [AI]
More:      [More]  →  [More, Device]  or  [More, Tools]
```

The **bar** shows the five tabs. The **rail** shows six destinations
(Dashboard, Files, Notes, Device, Tools, AI) and maps onto the same
model:

- Choosing **Device** on the rail selects the More tab and sets its
  stack to `[More, Device]`. Tools works the same way.
- A rail item is highlighted when its destination is the top of the
  current stack. Device is highlighted on `[More, Device]`.
- When the window changes from rail to bar (rotation, folding), the
  same state shows as "More" selected, with Device open inside it.

### 4.2 Rules

1. **Start:** Dashboard.
2. **Per-tab back stacks:** switching tabs restores where you were.
3. **Back** pops the current tab's stack. At the root of any tab other
   than Dashboard, back goes to Dashboard ("exit through home", as in
   the official Navigation 3 multiple-back-stacks recipe). At the
   Dashboard root, back leaves the app.
4. **Reselecting** the current bar tab returns it to its root, so
   reselecting More on `[More, Device]` goes to `[More]`. Reselecting
   the current rail item (for example Device while on Device) does
   nothing.
5. **State survives** rotation, dark-mode switches and process death:
   the selected tab and every stack are saved.
6. **Predictive back** gestures animate on Android 13+. At target SDK 37
   no manifest flag is needed; it's on by default. On Android 12 back
   works normally without the animation.
7. **The Dashboard links to all six areas.**

### 4.3 Implementation shape

- **`:core-navigation`** (pure Kotlin, no Android or Compose):
  - `Destination` keys and the five `TopLevel` tabs.
  - An immutable `NavState` with pure operations: `select(tab)`,
    `open(destination)`, `push`, `back()` (returns "exit" at the
    Dashboard root), and `current()`.
  - A small text codec for saving state. Unknown or corrupt saved
    state falls back to the start state.
- **`:app`** holds `NavState` in Compose state, saved with
  `rememberSaveable` and the codec. It feeds `current()` to
  `NavDisplay` and routes back gestures to `back()`.
- **Per-entry UI state** (scroll positions and so on) is kept by
  Navigation 3's saveable-state decorator. Placeholders have no such
  state, so tab-switch tests mainly cover the back stacks. Keeping
  per-entry state for tabs not on screen is a known detail to handle
  in Phase 4+, when screens get real state.

------------------------------------------------------------------------

## 5. Design system (`:core-ui`)

### 5.1 Theme

- `OpenopTheme { }`: Dynamic Color light/dark from the system setting.
  A user override comes with Settings (Phase 3).
- **Window theme:** a DayNight `themes.xml` so the window background
  matches dark mode before Compose draws (fixes the launch flash).
- **Typography and shapes:** Material 3 defaults with the system font.
  **No bundled fonts**, so no font licences.
- **Spacing tokens:** `Spacing.xs/s/m/l/xl` (4/8/16/24/32 dp).

### 5.2 Components (spec set, P2-D11)

| Component | Rule it encodes |
|---|---|
| `ScreenScaffold` | Consistent top app bar, insets, content padding |
| `LoadingState` | Consistent progress indicator + optional label |
| `EmptyState` | Icon, title, explanation, optional action. Used by every placeholder |
| `ErrorState` | Plain-language message + optional retry; never shows stack traces |
| `ConfirmDialog` / `DestructiveConfirmDialog` | Destructive actions always confirmed (§4.3 of CLAUDE.md); explicit verb ("Delete 3 files"), error colour, focus starts on Cancel |
| `PrimaryButton` / `SecondaryButton` / `DestructiveButton` | One visual hierarchy across the app |
| `InfoCard` | Title, body, optional trailing content |
| `ListRow` / `SectionHeader` | Consistent lists on top of Material `ListItem` |
| `LabeledTextField` | Label, supporting text and error text in one place |

Each component gets a `@Preview` (light + dark), a catalogue entry
(P2-D10) and at least one Compose UI test. Plain Material 3 components
are used directly everywhere else, without wrapping.

### 5.3 Placeholder screens

Each destination shows an `EmptyState` with the destination icon and
name, **"Planned for Phase N"** (Dashboard 4, Device 4, Files 5, Notes
6, AI 7, Tools 8), one sentence from CLAUDE.md, and **no buttons that
pretend to do something**. The Dashboard also shows shortcut cards to
all six areas (real navigation) and the Rust core footer (P2-D9). The
More screen lists Device and Tools.

------------------------------------------------------------------------

## 6. Accessibility

- Accessibility Test Framework checks on all UI tests (API 34+,
  P2-D8): touch targets, contrast, labels, traversal order, text size.
- Every icon has a content description or is marked decorative.
- Navigation items announce name and selected state to TalkBack.
- Layouts tested at **font scale 2.0** and in landscape: no clipped or
  overlapping text.
- Manual TalkBack pass on the phone, recorded in the Phase 2 report.

------------------------------------------------------------------------

## 7. Dependencies (new in Phase 2)

| Dependency | Needed for | Licence | Type | F-Droid notes |
|---|---|---|---|---|
| `androidx.navigation3:navigation3-runtime` + `-ui` 1.2.1 (P2-D2) | Navigation | Apache-2.0 | Runtime | AndroidX; standard |
| `androidx.compose.material3:material3-adaptive-navigation-suite` (BOM, 1.4.0) (P2-D3) | Bar/rail switching | Apache-2.0 | Runtime | AndroidX; standard |
| `androidx.compose.ui:ui-test-junit4-accessibility` (BOM, 1.12.1) + Accessibility Test Framework 4.1.1 (transitive) (P2-D8) | Accessibility checks | Apache-2.0 | Test-only | Not shipped |
| Material Symbols vector drawables, only those used (P2-D5) | Icons | Apache-2.0 | Vendored assets | Listed in `NOTICE` |
| Android Gradle Plugin, Kotlin Gradle Plugin, ktlint-gradle as **build-logic** dependencies (P2-D4) | Convention plugins | Apache-2.0 / MIT | Build-only | Same versions as today; just a different place |

No kotlinx.serialization, no network, no analytics, no proprietary
components. Every new artifact (including the build-logic classpath)
goes into `gradle/verification-metadata.xml`, regenerated from an empty
Gradle cache as in Phase 1.

------------------------------------------------------------------------

## 8. Tests

**JVM unit tests (`:core-navigation`)**

- Start state; tab switching keeps stacks; every §4.2 back and reselect
  rule; rail `open(Device)` and `open(Tools)`.
- Codec round trip; corrupt or unknown saved state falls back to start.

**Compose UI tests (CI emulators API 31 + 37, and the phone)**

- Every destination opens from the bar (phone size) and from the rail
  (wide window).
- Back behaviour and reselect behaviour match §4.2.
- **State restore:** activity recreation keeps the selected tab and
  stacks. For process death, the saved-state round trip goes through
  the real Compose saver. There's also a manual check with Android's
  "Don't keep activities" option on the phone.
- Placeholders show "Planned for Phase N" and no action buttons.
- Dashboard shortcuts open the right destination.
- Light and dark themes render.
- Each component renders and responds; the destructive dialog starts
  with focus on Cancel.
- Font scale 2.0 run of the navigation tests.
- Accessibility checks on all of the above (API 34+).

**CI changes:** `scripts/run-instrumented-tests.sh` gets the new
modules that have instrumented tests (`:core-ui`, `:app`), so the
0-tests check covers them too.

------------------------------------------------------------------------

## 9. Work breakdown (reviewable increments)

| # | Increment | Done when |
|---|---|---|
| 2.1 | `build-logic/` convention plugins (Android app/library, Compose, ktlint, Kotlin JVM, `openop.rust-bridge` with the Rust tasks moved out of `core-bridge/build.gradle.kts`); `:core-navigation` with its model and unit tests; `:core-ui` skeleton; verification metadata regenerated. **No empty feature modules yet** | Build, lint and every existing test unchanged and green; CI green |
| 2.2 | `OpenopTheme`, spacing tokens, DayNight window theme, vendored icons, `NOTICE` | App uses `OpenopTheme`; no light flash in dark mode; previews render |
| 2.3 | Components (§5.2) + component tests, then the debug catalogue (P2-D10) | Catalogue shows every component; tests pass |
| 2.4 | Feature modules with placeholders; app shell with Navigation 3 + `NavigationSuiteScaffold`; Dashboard shortcuts and core footer; shell UI tests | All destinations open on bar and rail; §4.2 rules covered by tests |
| 2.5 | Accessibility pass: checks on, font scale 2.0, landscape and wide window, TalkBack manual pass | All checks pass; findings recorded |
| 2.6 | `docs/design-system.md`, navigation section in `architecture.md`, phone verification, CI green, Phase 2 report | All acceptance criteria (§10) met |

Each increment is committed separately and ends with a buildable,
green project.

------------------------------------------------------------------------

## 10. Acceptance criteria (Phase 2 exit)

Status (2026-11-19). CI = x86_64 emulators, API 31 and API 37.

- [x] Every approved destination can be opened, from the bar (5 items) and
      from the rail (6 items). `ShellBarTest` and `ShellRailTest` pass in CI
      on API 31 and 37. *Not yet run on the phone.*
- [x] Navigation behaves consistently. The §4.2 rules are covered by 31
      unit tests (`:core-navigation`) and by UI tests for back, reselect,
      tab memory and shortcuts. State survives activity recreation
      (`MainActivityTest`) and the save/restore path used after process
      death (`StateRestorationTester`).
- [~] Light and dark themes with Dynamic Color render correctly.
      `OpenopThemeTest` checks the dynamic light/dark schemes in CI.
      Accessibility checks pass in dark mode. **Phone screenshots in both
      modes are pending.**
- [x] No module functionality is presented as complete. Every
      placeholder says "Planned for Phase N" and has no actions
      (`placeholdersOfferNoActions`). Dashboard shortcuts navigate for real.
- [x] Reusable components (spec set, P2-D11) exist, are documented in
      `docs/design-system.md`, and have 11 component tests (plus 3 theme
      tests).
- [~] Accessibility checks (Accessibility Test Framework, API 34+) pass
      on every screen: bar, rail, dark mode, font scale 2.0, real landscape
      rotation, and before every click in the component tests. **The manual
      TalkBack pass on the phone is pending.**
- [x] New dependencies listed with licences (§7). `NOTICE` covers
      vendored icons. Verification metadata was checked from an empty
      Gradle cache after each dependency change.
- [x] All Phase 1 checks still pass; CI green (`04cf5d1`).

------------------------------------------------------------------------

## 11. Not proposed for Phase 2 (kept for later)

- Screenshot (golden image) tests.
- User theme override and other settings (Phase 3).
- `ProgressRow` and the progress/cancellation pattern (Phase 3).
- Customisable or reorderable Dashboard (needs approval; CLAUDE.md
  §5.1).
- Translations.
- An About / licences screen (Phase 3 or Phase 10).

------------------------------------------------------------------------

## 12. Risks

| Risk | Handling |
|---|---|
| Navigation 3 is newer; fewer examples exist | Small surface: our own pure-Kotlin navigation model, unit-tested in isolation; the official recipes cover the patterns used |
| Moving to build-logic breaks the build or the Rust tasks | 2.1 changes no behaviour and must keep every existing test green before anything else starts. The Rust tasks move as-is |
| Included `build-logic` needs its own dependency-verification entries | Regenerate verification metadata from an empty cache and check that the build-logic classpath is covered |
| Accessibility checks need API 34+ | Run on the API 37 emulator and phone; skipped on API 31 |
| HyperOS install prompts slow phone testing | CI emulators carry the automated tests; the phone is for the manual visual and TalkBack pass |
| Dynamic Color produces low-contrast palettes on some wallpapers | Contrast checks + manual check with several wallpapers |

------------------------------------------------------------------------

## 13. Approval question

Decisions answered (§3.1). Phase 2 starts with increment 2.1 once the
project owner explicitly authorizes implementation.

------------------------------------------------------------------------

## 14. Changes in rev. 2

From an architecture review of the plan against the code, and an API
check against official sources (developer.android.com, AndroidX
release notes and sources, official Navigation 3 recipes), 2026-11-18:

- **P2-D2:** Navigation 3's `rememberNavBackStack` requires
  kotlinx.serialization; the plan now uses an app-owned back stack
  with a custom saver (officially supported). No new dependency.
- **P2-D3:** confirmed that `NavigationSuiteScaffold` 1.4.0 can show
  five items on the bar and six on the rail.
- **§4.1:** defined how the six-item rail maps onto the five-tab
  model, including what reselecting does on each.
- **§4.2 rule 6:** no manifest flag is needed for predictive back at
  target SDK 37.
- **P2-D8:** accessibility checks need API 34+; they're skipped on the
  API 31 emulator.
- **P2-D4:** added `:core-navigation` (pure Kotlin) and explicit
  module dependency rules. The Rust tasks move into a build-logic
  plugin.
- **§9:** feature modules are now created in 2.4, not 2.1 (empty
  modules would add nothing and could trip CI checks).
- **P2-D5:** vendor only icons that are used (lint fails on unused
  resources). Confirmed the material-icons libraries are unmaintained.
- **§5.1:** DayNight window theme to fix the dark-mode launch flash.
- **§8:** process-death testing made concrete.
- **New P2-D11:** full spec component set vs a minimal set.
  `ProgressRow` moves to Phase 3.
- **§2:** newer library versions were announced but aren't published
  yet; re-check at implementation start.

------------------------------------------------------------------------

## 15. Progress

| # | State | Notes |
|---|---|---|
| 2.1 | ✅ Done | `build-logic/` included build with convention plugins `openop.android.application`, `openop.android.library`, `openop.android.compose`, `openop.kotlin.jvm`, `openop.ktlint`, `openop.rust-bridge` (Rust tasks moved out of `core-bridge/build.gradle.kts`, behaviour unchanged). `:core-navigation` (pure Kotlin): `Destination`, `TopLevel`, immutable `NavState` (select/open/back, rail selection, displayed stack), `NavStateCodec`. Written test-first: 31 unit tests, every rule watched failing before implementation. `:core-ui` skeleton (`Spacing`). Release APK unchanged: 1,989,216 bytes before and after. Verification metadata regenerated from an empty Gradle cache (514 components). |
| 2.2 | ✅ Done | `OpenopTheme` (Dynamic Color light/dark); DayNight window theme using the system dynamic neutral palette (no launch flash); Compose test setup moved into `openop.android.compose`. |
| 2.3 | ✅ Done | Spec component set (P2-D11) with previews and component tests; debug-only "OPENOP Catalog" activity (absent from the release APK, checked). |
| 2.4 | ✅ Done | Feature modules (dashboard, files, notes, device, tools, ai); `NavigationSuiteScaffold` + Navigation 3 shell driven by `NavState`; More screen; 13 Material Symbols icons; `NOTICE`. Shell UI tests for bar, rail, back, reselect, restore and recreation. |
| 2.5 | ✅ Automated part done | Accessibility Test Framework checks on every screen (API 34+), font scale 2.0, dark mode, real landscape rotation. TalkBack manual pass pending (phone). |
| 2.6 | ⏳ Docs done; phone check pending | `docs/design-system.md`; `docs/architecture.md` updated for Phase 2. |

Changes made during 2.1 (beyond the plan):

- **Lifecycle pinned to 2.11.0** for every Compose module (constraint in
  `openop.android.compose`). Library modules otherwise resolved lifecycle
  2.9.4, whose bundled lint checks AGP 9.5 rejects
  (`ObsoleteLintCustomCheck`, an error under warnings-as-errors). The
  app moves from 2.10.0 to 2.11.0, the latest stable.
- The ktlint exclude for generated sources now applies to every module
  (it only matters where code is generated).
- `open(root)` on a tab that is already selected but deep now returns
  that tab to its root (found in review; spec §4.3 "show destination").
  `open` also covers the planned `push`.
- Corrupt saved state with duplicate entries now falls back to the
  start state instead of being accepted (found in review).

Found and fixed during 2.2–2.5 (each found by CI or review, not by luck):

- **Stale Dashboard footer (app bug).** NavDisplay keeps each entry's
  content lambda, so the footer kept showing "Connecting…" after the Rust
  call returned. Fixed with `rememberUpdatedState`; covered by
  `MainActivityTest`.
- **Destructive dialog focus.** The focus request now runs in the dialog's
  own window. It only applies outside touch mode, and the test checks it
  that way.
- **Malformed icon XML.** Removing the AppCompat-only `android:tint` line
  also removed the `>` closing `<vector>`; repaired and documented in
  `design-system.md`.
- **Weak tests.** Shell assertions used substrings that also appear on
  Dashboard cards (review finding); they now match placeholder-only text.
  Overlapping test layouts and ambiguous labels in the component tests were
  fixed, and back presses in tests wait for pending recomposition.
- **Invalid landscape test.** A forced window size rescales density, which
  distorted the touch-target checks; the test now really rotates the
  activity.
- **Small design-system changes:** `:core-ui` depends on the pure-Kotlin
  `:core-navigation` (one mapping for labels, icons and phases);
  `ListRow` announces a button role; `currentWindowAdaptiveInfoV2` is used
  (V1 is deprecated).

Open question for the owner (from review, not changed): on the **rail**,
Back from Device or Tools goes to the More screen (spec §4.2, since Device
and Tools live under More). The rail has no "More" item, so nothing is
highlighted there. An alternative is to skip the More screen on the rail
and go straight to Dashboard. Changing this needs approval.
