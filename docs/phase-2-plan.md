# OPENOP — Phase 2 Implementation Plan

> **Phase:** 2 — Application shell and design system
> **Status:** Draft — awaiting project-owner approval
> **Date:** 2026-11-16
> **Scope source:** `CLAUDE.md` §4 and §10 (Phase 2)
> **Builds on:** Phase 1 (complete, CI green at `83fd8de`)

This document is a plan only. It does not authorize code changes.
Section 3 lists the decisions the project owner needs to make before
Phase 2 can start. Several of them (navigation structure, labels, app
icon) are ones CLAUDE.md says must not be decided silently.

------------------------------------------------------------------------

## 1. Goal

When Phase 2 is done, OPENOP is a navigable app shell:

- every approved destination (Dashboard, Files, Notes, Device, Tools,
  AI) can be opened;
- navigation and the back button behave consistently, keep their state,
  and survive rotation and process death;
- light and dark themes with Dynamic Color render correctly;
- a small set of reusable components (states, dialogs, buttons, cards,
  lists, inputs) exists and is documented;
- layouts adapt to phones, foldables and tablets;
- basic accessibility checks run in the tests.

**No module functionality.** Every destination shows an honest
placeholder ("Planned for Phase N") and nothing that looks like a
working feature (CLAUDE.md Phase 2 exit criterion).

### Out of scope for Phase 2

- Settings, persistence, Room, the permission coordinator (Phase 3).
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
- CI: Rust, Android, emulator tests on API 31 and 37.

------------------------------------------------------------------------

## 3. Decisions needed before implementation

### P2-D1 — Navigation structure for six destinations ⚠️ main decision

CLAUDE.md asks for bottom navigation with six destinations, and says
to propose an alternative if six is crowded. Material 3 recommends
**3–5** items in a navigation bar. On a typical phone (360–412 dp
wide), six items leave about 60–68 dp each. The labels "Dashboard" and
"Notes" then get truncated or need smaller text, and touch targets
shrink towards the 48 dp minimum.

| Option | Phone layout | Pros | Cons |
|---|---|---|---|
| **A. Six-item bar** | Dashboard · Files · Notes · Device · Tools · AI | Matches the spec literally; every area one tap away | Outside Material guidance; cramped labels; harder at large font sizes (accessibility) |
| **B. Five items with "More"** (recommended) | Dashboard · Files · Notes · AI · **More** → Device, Tools | Within guidance; readable labels at large font sizes; the Dashboard also links to all six | Device and Tools take two taps (or one from the Dashboard) |
| **C. Four items with "More"** | Dashboard · Files · Notes · **More** → Device, Tools, AI | Roomiest | AI is a core module; hiding it seems wrong |
| **D. Navigation drawer** | Hamburger menu | Unlimited items | Not bottom navigation; hides everything behind a tap; against the approved decision |

On wide screens (tablets, unfolded foldables, landscape), every option
switches automatically to a **navigation rail**, which shows all six
directly (see P2-D3).

**Recommendation: B.** Device and Tools are the least frequently used
areas, and they stay one tap away from the Dashboard. Which two go
under "More" is your call.

### P2-D2 — Navigation library

| Option | Version (checked 2026-11-16) | Notes |
|---|---|---|
| **Navigation 3** (`androidx.navigation3`) (recommended) | 1.2.1 stable | Built for Compose. The back stack is ordinary state the app owns, which makes per-tab back stacks and tests simple. Google's current recommendation for new Compose apps |
| Navigation Compose (`androidx.navigation:navigation-compose`) | 2.10.2 stable | Mature and widely documented; route-based NavController API; per-tab back stacks are more awkward |

Both are Apache-2.0 and FLOSS. **Recommendation: Navigation 3.** Back
stacks use small Kotlin key types with a custom saver, so no
kotlinx.serialization dependency is needed in Phase 2.

### P2-D3 — Adaptive navigation (bar / rail)

Use `androidx.compose.material3:material3-adaptive-navigation-suite`
(1.4.0, already managed by the Compose BOM). Its
`NavigationSuiteScaffold` shows a bottom bar on phones and a
navigation rail on wide windows.

**Recommendation: yes.** It's the standard way to meet "responsive
layouts" (CLAUDE.md §4.2) without custom layout code.

### P2-D4 — Module structure

| Option | Structure | Pros | Cons |
|---|---|---|---|
| **Feature modules** (recommended) | `:core-ui` (theme + components), `:feature:dashboard`, `:feature:files`, `:feature:notes`, `:feature:device`, `:feature:tools`, `:feature:ai`, plus `build-logic/` convention plugins | Gradle enforces boundaries between modules (CLAUDE.md §1.2 "modular design"); features build and test separately; sets the pattern for Phases 4–8 | More Gradle files (kept small by the convention plugins); slightly slower first build |
| Packages in `:app` | `app/.../feature/<name>` | Fewer files | Boundaries only by convention; harder to split later |

**Recommendation: feature modules.** `build-logic/` holds the shared
Android/Kotlin/ktlint/lint settings that are currently copied between
`app/` and `core-bridge/`. This is an architecture change, which is why
it's listed here.

### P2-D5 — Icons

| Option | Notes |
|---|---|
| **Vendored Material Symbols as vector drawables** (recommended) | Apache-2.0. Only the ~15 icons we use are copied in (outlined + filled pairs for selected state). No runtime dependency, tiny APK cost. Attribution goes in `NOTICE` |
| `material-icons-core` | Frozen at 1.7.8 and no longer updated by Google; small set; missing some icons we need |
| `material-icons-extended` | Frozen too, and very large unless shrunk |

**Recommendation: vendored Material Symbols.**

### P2-D6 — Destination labels (open "naming of individual modules")

Proposed labels, taken from the spec: **Dashboard, Files, Notes,
Device, Tools, AI**, plus **More** if P2-D1 = B or C. An alternative is
"Home" instead of "Dashboard", which is shorter for the bar.

**Recommendation:** keep the spec names. They're easy to change later
because they live in string resources.

### P2-D7 — App icon and brand colour (open "final visual branding")

Dynamic Color takes the app's colours from the wallpaper on every
supported device (minSdk 31), so a brand colour is only needed for the
launcher icon, screenshots and previews.

| Option | Notes |
|---|---|
| **Keep deferring** (recommended unless you have a design) | Launcher keeps the generic Android icon; lint check stays disabled |
| Neutral placeholder icon | A simple geometric adaptive icon, clearly marked temporary |
| You provide or choose an icon | Added as an adaptive icon (foreground + background + monochrome for themed icons) |

### P2-D8 — Accessibility checks in tests

Add `androidx.compose.ui:ui-test-junit4-accessibility` (test-only,
Apache-2.0, version from the Compose BOM). It runs Google's
Accessibility Test Framework checks (touch target size, contrast,
missing labels) automatically during Compose UI tests.

**Recommendation: yes.**

### P2-D9 — Where the Rust core demo goes

The Phase 1 placeholder shows "Rust core 0.1.0 (debug)".

| Option | Notes |
|---|---|
| **Small footer on the Dashboard placeholder** (recommended) | Keeps the end-to-end bridge check visible and tested; replaced by the real Dashboard in Phase 4 |
| An "About" screen | Needs a new entry point (top bar menu); better done with Settings in Phase 3 |
| Remove it | Loses the visible bridge check; instrumented tests still cover it |

### P2-D10 — Debug-only component catalogue

A screen listing every design-system component in light and dark,
built into **debug builds only** and opened from the launcher as
"OPENOP Catalog" or with `adb shell am start`. It makes visual review
of Phase 2 easy and isn't in release builds.

**Recommendation: yes.** Small cost.

------------------------------------------------------------------------

## 4. Navigation behaviour (shown for option B)

```text
            ┌──────────── App shell (NavigationSuiteScaffold) ────────────┐
            │ phone: bottom bar          wide window: navigation rail     │
            │                                                             │
 Dashboard ─┤  back stack [Dashboard]                                     │
 Files ─────┤  back stack [Files]                                         │
 Notes ─────┤  back stack [Notes]                                         │
 AI ────────┤  back stack [AI]                                            │
 More ──────┤  back stack [More] → [More, Device] / [More, Tools]         │
            └─────────────────────────────────────────────────────────────┘
```

Rules:

1. **Start destination:** Dashboard.
2. **Each top-level destination keeps its own back stack.** Switching
   tabs restores where you were.
3. **Back:** pops the current tab's stack. At the root of any tab other
   than Dashboard, back goes to Dashboard. At the Dashboard root, back
   leaves the app.
4. **Reselecting the current tab** returns it to its root.
5. **State survives** rotation, dark-mode switches and process death:
   the selected tab and all back stacks are saved.
6. **Predictive back** gestures animate correctly (enabled in the
   manifest).
7. **The Dashboard links to all six areas**, so Device and Tools are
   one tap away from the start screen.

The back-stack logic lives in a plain Kotlin class with JVM unit
tests, separate from the UI.

------------------------------------------------------------------------

## 5. Design system (`:core-ui`)

### 5.1 Theme

- `OpenopTheme { }`: Dynamic Color light/dark from the system setting.
  A user override (light/dark/system) comes with Settings in Phase 3.
- **Typography:** Material 3 defaults with the system font. **No
  bundled fonts**, so no font licences.
- **Shapes:** Material 3 defaults.
- **Spacing tokens:** `Spacing.xs/s/m/l/xl` (4/8/16/24/32 dp) so
  screens don't hard-code padding.
- **Edge-to-edge:** already on; components handle insets.

### 5.2 Components

Only where they encode an OPENOP rule. Plain Material 3 components are
used directly everywhere else, without wrapping.

| Component | Rule it encodes |
|---|---|
| `ScreenScaffold` | Consistent top app bar, insets, content padding |
| `LoadingState` | Consistent progress indicator + optional label |
| `EmptyState` | Icon, title, explanation, optional action. Used by every placeholder |
| `ErrorState` | Plain-language message + optional retry; never shows stack traces |
| `ConfirmDialog` / `DestructiveConfirmDialog` | Destructive actions always confirmed (§4.3); explicit verb ("Delete 3 files"), error colour, focus starts on Cancel |
| `PrimaryButton` / `SecondaryButton` / `DestructiveButton` | One visual hierarchy across the app |
| `InfoCard` | Title, body, optional trailing content (Dashboard, Device) |
| `ListRow` / `SectionHeader` | Consistent lists (Files, Notes, Device) on top of Material `ListItem` |
| `LabeledTextField` | Label, supporting text and error text in one place |
| `ProgressRow` | Determinate/indeterminate progress with optional Cancel (§4.3: progress for long operations) |

Every component gets a `@Preview` in light and dark, an entry in the
debug catalogue (P2-D10), and at least one Compose UI test.

### 5.3 Placeholder screens

Each destination shows an `EmptyState` with:

- the destination icon and name;
- **"Planned for Phase N"** (Dashboard 4, Device 4, Files 5, Notes 6,
  AI 7, Tools 8);
- one sentence from CLAUDE.md describing the module's purpose;
- **no buttons that pretend to do something.**

The Dashboard placeholder also shows shortcut cards to all six areas
(these do work: they navigate) and the Rust core footer (P2-D9).

------------------------------------------------------------------------

## 6. Accessibility

- Touch targets ≥ 48 dp (Material 3 default; checked by ATF, P2-D8).
- Every icon either has a content description or is marked decorative.
- Navigation items have TalkBack labels and announce the selected
  state.
- Layouts tested at **font scale 2.0** and in landscape: no clipped or
  overlapping text.
- Colour contrast comes from Dynamic Color (Material tonal palettes)
  and is checked by ATF.
- Manual pass with TalkBack on the phone, recorded in the Phase 2
  report.

------------------------------------------------------------------------

## 7. Dependencies (new in Phase 2)

| Dependency | Needed for | Licence | Type | F-Droid notes |
|---|---|---|---|---|
| `androidx.navigation3:navigation3-runtime` + `-ui` 1.2.1 (P2-D2) | Navigation | Apache-2.0 | Runtime | AndroidX; standard |
| `androidx.compose.material3:material3-adaptive-navigation-suite` (BOM) (P2-D3) | Bar/rail switching | Apache-2.0 | Runtime | AndroidX; standard |
| `androidx.compose.ui:ui-test-junit4-accessibility` (BOM) (P2-D8) | Accessibility checks in tests | Apache-2.0 | Test-only | Not shipped |
| Material Symbols vector drawables, ~15 icons (P2-D5) | Icons | Apache-2.0 | Vendored assets | Attribution in `NOTICE` |

No network, analytics or proprietary components. As in Phase 1, every
new artifact goes into `gradle/verification-metadata.xml`, regenerated
from an empty Gradle cache.

------------------------------------------------------------------------

## 8. Tests

**JVM unit tests**

- Back-stack logic: start state, tab switching keeps stacks, back
  rules (§4 rules 3–4), save/restore round trip.

**Compose UI tests (emulators in CI + phone)**

- Every destination opens from the navigation bar; on a wide window,
  from the rail.
- Back behaviour matches §4.
- Tab state survives switching tabs and activity recreation.
- Every placeholder shows "Planned for Phase N" and no action buttons.
- Dashboard shortcuts open the right destination.
- Theme renders in light and dark (key colours come from the dynamic
  scheme).
- Components: each renders and responds; the destructive dialog starts
  with focus on Cancel.
- Accessibility checks enabled on all of the above (P2-D8).
- Font scale 2.0 run of the navigation tests.

**Kept from Phase 1:** bridge tests, the native-library checks, and the
results check that fails on 0 tests.

------------------------------------------------------------------------

## 9. Work breakdown (reviewable increments)

| # | Increment | Done when |
|---|---|---|
| 2.1 | `build-logic/` convention plugins; `:core-ui` and `:feature:*` module skeletons; `app/` and `core-bridge/` switched to the plugins (P2-D4) | Build, lint and all tests unchanged and green; CI green |
| 2.2 | `OpenopTheme`, spacing tokens, vendored icons + `NOTICE` | App uses `OpenopTheme`; previews render light and dark |
| 2.3 | Components (§5.2) + debug catalogue (P2-D10) + component tests | Catalogue shows every component; tests pass |
| 2.4 | Navigation shell: Navigation 3 + `NavigationSuiteScaffold`, back-stack class, placeholders, Dashboard shortcuts and core footer | All destinations open; §4 rules covered by tests |
| 2.5 | Accessibility pass: ATF checks on, font scale 2.0, landscape and wide-window checks, TalkBack manual pass | All checks pass; findings recorded |
| 2.6 | `docs/design-system.md`, navigation section in `architecture.md`, phone verification, CI green, Phase 2 report | All acceptance criteria (§10) met |

Each increment is committed separately and ends with a buildable,
green project.

------------------------------------------------------------------------

## 10. Acceptance criteria (Phase 2 exit)

From CLAUDE.md §10, made concrete:

- [ ] Every approved destination can be opened (bar on phone, rail on
      wide window), tested in CI on API 31 and 37 and on the phone.
- [ ] Navigation behaves consistently: the §4 rules are covered by
      tests, and state survives rotation and process death.
- [ ] Light and dark themes with Dynamic Color render correctly
      (tests + phone screenshots in both modes).
- [ ] No module functionality is presented as complete: every
      placeholder says "Planned for Phase N" and has no fake actions.
- [ ] Reusable components exist, are documented in
      `docs/design-system.md`, and have tests.
- [ ] Accessibility checks pass in all UI tests; font scale 2.0 and
      landscape show no clipping; TalkBack pass recorded.
- [ ] New dependencies listed with licences; `NOTICE` covers vendored
      icons; verification metadata regenerated from an empty cache.
- [ ] All Phase 1 checks still pass; CI green.

------------------------------------------------------------------------

## 11. Not proposed for Phase 2 (kept for later)

Listed so they aren't added silently (CLAUDE.md §14.7):

- Screenshot (golden image) tests, e.g. AGP's Compose Preview
  Screenshot Testing.
- User theme override and other settings (Phase 3).
- Customisable or reorderable Dashboard (needs approval; CLAUDE.md
  §5.1).
- Translations.
- An About / licences screen (with Settings in Phase 3 or release
  prep in Phase 10).

------------------------------------------------------------------------

## 12. Risks

| Risk | Handling |
|---|---|
| Navigation 3 is newer, so fewer examples exist | Small surface: our own back-stack class, tested in isolation; Navigation Compose stays the fallback |
| Six destinations still feel crowded with option A | Font-scale 2.0 tests show it early; can switch to B without changing the back-stack logic |
| Feature modules slow the build | Convention plugins plus Gradle and configuration caches; measured and reported in 2.1 |
| HyperOS install prompts slow phone testing | CI emulators cover API 31 and 37; the phone is used for the manual TalkBack and visual pass |
| Dynamic Color produces low-contrast palettes on some OEM wallpapers | ATF contrast checks; manual check with several wallpapers on the phone |

------------------------------------------------------------------------

## 13. Approval question

Please answer **P2-D1 to P2-D10** (§3). For P2-D1 and P2-D6/D7, these
are open product decisions in CLAUDE.md. Once you approve the plan and
answer the decisions, Phase 2 starts with increment 2.1 after your
explicit go-ahead.
