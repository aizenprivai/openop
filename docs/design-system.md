# OPENOP — Design system

Everything here lives in `:core-ui` (package
`io.github.aizenprivai.openop.designsystem`). Browse every component on a
device with the debug-only **OPENOP Catalog** launcher entry (debug builds
only), or with the `@Preview`s in `ComponentPreviews.kt`.

## Theme

- **`OpenopTheme { … }`** wraps every screen. Colours come from the
  wallpaper (Material 3 Dynamic Color); minSdk 31 means every supported
  device has it. Light or dark follows the system setting (a user override
  comes with Settings in Phase 3).
- **Typography and shapes:** Material 3 defaults with the system font. No
  fonts are bundled.
- **Window theme** (`app/src/main/res/values*/themes.xml`): the background
  before Compose draws uses the system's dynamic neutral palette, so there is
  no wrong-colour flash at launch in dark mode.

## Spacing

Use `Spacing` instead of raw dp values:

| Token | Value | Typical use |
|---|---|---|
| `Spacing.xs` | 4 dp | Tight gaps inside a component |
| `Spacing.s` | 8 dp | Between related items |
| `Spacing.m` | 16 dp | Screen padding, between cards |
| `Spacing.l` | 24 dp | Around empty/error states |
| `Spacing.xl` | 32 dp | Large separations |

## Components

Use plain Material 3 components directly unless one of these fits. They
exist because each one encodes an OPENOP rule.

| Component | Use it for | Rule it encodes |
|---|---|---|
| `ScreenScaffold(title) { padding -> }` | Every screen | Consistent top app bar and insets |
| `LoadingState(label)` | Work in progress | Indicator always labelled for screen readers |
| `EmptyState(title, body, icon, action)` | Nothing to show yet | Explains what will appear; optional action |
| `ErrorState(message, onRetry)` | Recoverable failures | Plain-language message only; never stack traces (log those with `AppLog`) |
| `ConfirmDialog` | Confirming a normal action | Explicit confirm label |
| `DestructiveConfirmDialog` | **Every** destructive action (CLAUDE.md §4.3) | Explicit verb ("Delete 3 files"), error colour, focus starts on Cancel for keyboard/D-pad users |
| `PrimaryButton` / `SecondaryButton` | Main / secondary actions | One visual hierarchy |
| `DestructiveButton` | Starting a destructive action | Always followed by `DestructiveConfirmDialog` |
| `InfoCard(title, body, icon, onClick)` | Summaries, shortcuts | With `onClick`, the whole card is one touch target |
| `ListRow(headline, supporting, icon, onClick)` | Rows in lists | Whole row clickable, announced as a button |
| `SectionHeader(text)` | Above groups of rows/cards | Announced as a heading |
| `LabeledTextField(value, onValueChange, label, supportingText, errorText)` | Text input | Visible label; error text replaces the hint and is announced as an error |
| `AreaPlaceholder(destination)` | A module that isn't built yet | "Planned for Phase N" + purpose, **no actions** (nothing may look like a working feature) |

## Destinations and icons

`Destination.visuals` (in `DestinationVisuals.kt`) gives each destination
its label, one-line purpose, icon, selected icon and planned phase. The
navigation bar/rail, the Dashboard shortcuts, the More screen and the
placeholders all read from it, so a label or icon changes in one place.

Icons are **Material Symbols (Outlined)** vector drawables
(`core-ui/src/main/res/drawable/openop_ic_*.xml`, Apache-2.0, listed in
`NOTICE`). To add one:

1. Download the Android XML from
   `https://github.com/google/material-design-icons` →
   `symbols/android/<name>/materialsymbolsoutlined/<name>_24px.xml`
   (and `<name>_fill1_24px.xml` for the selected state).
2. Remove the `android:tint="?attr/colorControlNormal"` attribute (it needs
   AppCompat, which OPENOP doesn't use) and **keep the `>` that closes the
   `<vector>` tag**. Add the attribution comment inside `<vector>`.
3. Name it `openop_ic_<name>.xml`. Lint fails on unused resources, so add an
   icon in the same change as the code that uses it.

## Accessibility rules

- Icons next to a text label are decorative (`contentDescription = null`);
  icons alone need a description.
- Clickable cards and rows are single touch targets with a role.
- Headings use `SectionHeader` (heading semantics).
- Every UI test runs Google's Accessibility Test Framework checks on API 34+
  (`enableAccessibilityChecks()`), including at font scale 2.0, in dark mode
  and in a landscape window.
