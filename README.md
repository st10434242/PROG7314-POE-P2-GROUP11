# RUNWAY

A digital wardrobe app for Android. Photograph your clothes, build outfits on a
drag-and-drop canvas, plan a week against the weather, track cost-per-wear, and
swap or donate what you no longer wear.

Native Kotlin on an MVVM architecture, with Room for local persistence and a
custom cloud-hosted REST API. Built for PROG7314 (Part 2 of the POE) from the
design specification and clickable prototype produced in Part 1.

## Stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.3.10 |
| UI | Android view system, XML layouts, Material 3 (`com.google.android.material` 1.14.0) |
| View access | ViewBinding — no `findViewById` in the codebase |
| Architecture | MVVM with unidirectional data flow |
| Persistence | Room 2.8.4 (via KSP) |
| Navigation | Navigation component, single Activity + XML nav graph |
| Build | AGP 9.3.2 / Gradle 9.5, Kotlin DSL + version catalog |
| Min SDK | 26 (Android 8.0), compileSdk 37 |

### Why XML rather than Jetpack Compose

The team chose the view system deliberately. The decision was taken while the
repository still held only scaffolding, so nothing of substance was rewritten:
the domain, data and DI layers are UI-agnostic and were carried across untouched,
along with their unit tests.

## The Runway component library

The UI is built on a shared component library ported from the Part 1 prototype,
rather than on per-screen layouts. Every screen in the app draws from it, which is
what keeps twenty-odd screens looking like one product when four people are
building them in parallel.

> **Building a screen? Read [howto-use-components.md](howto-use-components.md).**
> It covers every component with copy-paste examples.

![Component catalog in Day and Night](docs/screenshots/catalog-day.png)

### Design tokens

`res/values/colors.xml` defines the palette under **semantic** names — `rw_accent`,
`rw_surface`, `rw_muted` — and `res/values-night/colors.xml` redefines exactly that
set. Because the names are semantic rather than literal, there is a single
`themes.xml` covering both schemes instead of a Day copy and a Night copy.

| Token | Day | Night |
|---|---|---|
| `rw_bg` | `#FFFFFF` | `#0A0A0B` |
| `rw_surface` | `#F5F5F6` | `#161618` |
| `rw_elevated` | `#FFFFFF` | `#1F1F22` |
| `rw_border` | `#E5E5E7` | `#2A2A2E` |
| `rw_fg` | `#0A0A0B` | `#FAFAFA` |
| `rw_muted` | `#6B6B70` | `#A1A1A6` |
| `rw_accent` | `#CE0E2D` | `#FF3049` |

`Theme.Runway` maps those tokens onto Material 3's own theme attributes
(`colorPrimary`, `colorOnSurfaceVariant`, `colorOutline`, …). That mapping is the
load-bearing part: a plain `MaterialButton`, `Chip` or `TextInputLayout` dropped
into any layout is already on-brand, so component styles only have to describe
what genuinely differs from the Material default.

Tonal elevation is switched off (`elevationOverlayEnabled=false`). Material 3
lifts an elevated surface by blending `colorSurfaceTint` into it, which with a red
primary turns every sheet and dialog pink. Runway separates surfaces with a
hairline border and no shadow, so the overlay is disabled outright.

### Components

Anything expressible as a style **is** a style, in `res/values/styles.xml` — a
style costs no class, no inflation and no lifecycle. Custom views exist only where
the composition cannot be expressed declaratively.

| Styles | Custom views (`ui/components/`) |
|---|---|
| `Widget.Runway.Button` ×5 variants ×3 sizes | `ScreenHeaderView` |
| `Widget.Runway.Button.Icon` | `SectionHeaderView` |
| `Widget.Runway.Chip` | `EmptyStateView` |
| `Widget.Runway.Card` | `ListRowView` |
| `Widget.Runway.TextField` | `StatTileView` |
| `Widget.Runway.Switch` | `StarRatingView` |
| `Widget.Runway.Meter` | `AvatarView` |
| `Widget.Runway.Badge` ×3 tones | `SwatchView` |
| `Widget.Runway.SegmentedButton` | `ProgressRingView` |
| `Widget.Runway.ScreenContent` / `.Rail` / `.ActionBar` | `FieldView`, `SyncBannerView` |

Plus three helpers: `RunwayToast` (the dark confirmation pill), `RunwayDialogs`
(confirm and destructive dialogs) and `RunwayBottomSheet` (a base class supplying
the handle, corners, close button and optional pinned footer).

The 43 icons the prototype actually uses are ported as vector drawables
(`ic_rw_*.xml`), tinted from the theme so they invert correctly in Night.

### Component catalog

`CatalogActivity` renders every component and variant on one screen, reachable
from a button on any tab. It is the library's test surface — a component that
looks wrong there looks wrong everywhere — and it is what the screenshots in this
README are taken from.

![The app shell](docs/screenshots/shell-tabs.png)
![Bottom sheet](docs/screenshots/bottom-sheet.png)

## Architecture

```
ui/
  components/     the design system: custom views + helpers
  catalog/        CatalogActivity - every component on one screen
  home/ wardrobe/ outfits/ profile/   one package per tab
  navigation/     nav-graph argument names
domain/           plain Kotlin models + repository interfaces
data/
  local/          Room entities, DAOs, database
  mapper/         entity <-> domain
  repository/     repository implementations
di/               AppContainer - manual dependency graph
```

`MainActivity` is the app's only Activity. It hosts a `NavHostFragment` and owns
what outlives any one screen: the bottom navigation, the add-item FAB and the
connectivity banner. Screens are Fragments; they never render the tab bar
themselves.

The domain layer is deliberately plain Kotlin — no Room annotations, no Android
imports — so the database schema can change without touching the UI.

### Logging

`Log.d` traces cover the Activity and Fragment lifecycles, navigation
destination changes, and user actions. Switching tabs runs `onDestroyView`
without `onDestroy`, and the logs make that visible — which is the distinction
behind the view-binding release in every Fragment.

## Building

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
```

Requires JDK 17+ and the Android SDK. `local.properties` is machine-specific and
git-ignored; create it with `sdk.dir` pointing at your SDK if Android Studio has
not already.
