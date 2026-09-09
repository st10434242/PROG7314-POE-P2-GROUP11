# Navigation graph & fragment stubs — implementation report

Scope: build the Android nav graph and a stub fragment/Kotlin file for every screen in the
Runway mockup (`runway/`), fix whatever was already broken, and check `runway/docs/NAVIGATION.md`
against the real mockup source rather than trusting it blindly. Fragments are intentionally
**stubs** (title + body placeholder, reusing the existing `StubFragment`/`fragment_stub.xml`
pattern) — no pixel-accurate screens were built.

## 1. Auditing NAVIGATION.md against the real mockup source

The doc is mostly accurate but has a few real discrepancies, found by reading the actual
mockup code (`src/nav/types.ts`, `src/nav/NavProvider.tsx`, `src/screens/registry.tsx`,
`src/App.tsx`, `src/shell/TabShell.tsx`) rather than the flow diagram:

- **"Planner" is not a separate screen.** The doc's flow diagram shows `Outfits --> Planner`
  as a pushed node, but there is no `planner` entry in `ScreenKey` at all. Planner is a
  segmented-control tab *inside* `OutfitsScreen.tsx` (`segment: 'outfits' | 'planner'`), not a
  navigable destination. The nav graph therefore has **no** `plannerFragment` — the day-detail
  sheet (`daySheet`) is opened directly from `OutfitsFragment`.
- **"List for Swap" is mislabeled as a modal.** The diagram colours `ListSwap` as a
  modal/sheet reached from Item Detail, but the real `SheetKey` union
  (`filter | sort | daySheet | pickOutfit | confidence | confirm`) has no such sheet. It's
  actually the `createListing` **screen** (a normal push destination, confirmed via
  `actions.createListing(listing)` in `SwapScreens.tsx`). The doc's own screen-inventory table
  doesn't list `createListing` at all — it's missing from the table entirely. Implemented as
  `createListingFragment`, a normal push destination, not a sheet.
- **`sort` and `confirm` sheets exist in code but aren't in the diagram.** Both are real
  `SheetKey` values with no mention in `NAVIGATION.md`. Both are now stub destinations
  (`sortSheet`, `confirmSheet`).
- Everything else in the doc's screen inventory (28 screens) checked out against
  `registry.tsx`/`types.ts`.

## 2. What was already broken (fixed)

- **The nav graph didn't compile.** `runway_nav_graph.xml` declared
  `app:startDestination="@id/homeFragment"` but defined **no destinations at all** — an
  invalid graph.
- **Six fragments referenced layout files that no longer exist in `res/layout`** (only stale
  copies remained under `build/`, from before someone deleted the XML): `item_detail_Fragment`,
  `Capture_Add_Item_Fragment`, `Capture_Item_Fragment`, `Background_Removal_Fragment`,
  `Tag_item_Fragment`, `Smart_colour_matcher_Fragment`. Each was unfinished Android-Studio
  "blank fragment" boilerplate (`newInstance(param1, param2)`, `ui.Capture` package casing,
  `item_detail_Fragment` lowercase class name) that would fail `compileDebugKotlin` the moment
  it was touched. All six were rewritten as clean stubs and renamed to match convention
  (see §4); `Capture_Add_Item_Fragment` had no real counterpart in the mockup's screen list and
  was dropped rather than renamed.
- **`fragment_login.xml` was dead code** — unused Android-Studio "blank fragment" boilerplate
  (`hello_blank_fragment`), never referenced by the real `LoginFragment`. Deleted, along with
  the now-orphaned `hello_blank_fragment` string.
- **`LoginFragment` referenced a view, `authEmailField`, that didn't exist in its own layout**
  (`fragment_auth_login.xml`) — another pre-existing compile error the moment that fragment was
  touched. Added the missing `FieldView` for the optional email hint, matching what the
  Kotlin code already expected (`placeholder`, `.editText.inputType`, `.text`, `.error`).
- **Splash, Onboarding and Biometric Unlock didn't exist at all.** `AuthActivity` jumped
  straight to the Google sign-in screen with no `NavHost`, even though all three are required
  screens per the spec and the mockup's `AppGate`.

## 3. Architecture decisions (per your answers)

- **One nav graph, one Activity.** `AuthActivity` is gone. `MainActivity` is now the sole,
  launcher Activity and hosts every destination — auth included — in
  `runway_nav_graph.xml`. `MainActivity` hides the bottom bar + FAB on auth and add-item
  destinations via a `CHROMELESS_DESTINATIONS` set checked in
  `addOnDestinationChangedListener`, matching the mockup (`TabBar` only renders inside
  `TabShell`, after `AppGate` hands off).
- **Add-item flow is a nested graph** (`addItemGraph`, start destination `cameraFragment`)
  inside the same `NavHostFragment`, entered from the FAB via
  `navController.navigate(R.id.addItemGraph)`. `TagItemFragment`'s continue action pops the
  whole nested graph (`popUpTo addItemGraph inclusive=true`) and lands on `itemDetailFragment`
  in whichever tab was active — matching "modal stack over the whole app" from the doc.
- **Bottom sheets are included** as six stub `<dialog>` destinations (`filterSheet`,
  `sortSheet`, `dayDetailSheet`, `pickOutfitSheet`, `confidenceSheet`, `confirmSheet`), each a
  `StubBottomSheet` (new shared base class, mirrors `StubFragment` but for
  `RunwayBottomSheet`/`BottomSheetDialogFragment`).
- **Sign-in screen renamed and reviewed.** `LoginFragment` → `SignInFragment` (package
  `ui.auth`, matches the `signin` route key), layout `fragment_auth_login.xml` →
  `fragment_signin.xml`. Its real Google-SSO logic (validation, session persistence, error
  states) is unchanged and preserved — only the destination it navigates to on success changed
  (from `startActivity(MainActivity)` to `findNavController().navigate(action_signIn_to_biometric)`),
  and the missing `authEmailField` bug above was fixed.
- **Sign-out fixed to match.** `ProfileFragment.signOut()` previously started the
  now-deleted `AuthActivity`; it now navigates via
  `action_profile_signOut_to_signIn` (`popUpTo splashFragment inclusive=true`).

### Splash/auth routing implemented

`SplashFragment` reads `AuthSessionStore.currentSession` (same store as before) and, after a
1.2s delay, routes a returning user straight to `biometricFragment` or a first-time user to
`onboardingFragment` — both with `popUpTo splashFragment inclusive=true` so splash never
sits on the back stack. This matches the doc's `Splash -. returning user .-> Bio` edge.
`OnboardingFragment` → `SignInFragment` → `BiometricFragment` → tab shell (`popUpTo splashFragment
inclusive=true`) completes the chain. `settings.biometrics`-style conditional skipping from the
mockup's `AppGate` was **not** replicated (there's no such preference stored in the Android app
yet) — Sign In always proceeds to Biometric Unlock for now; flagged here as a simplification.

## 4. Full destination inventory

Existing, unchanged (real logic preserved): `HomeFragment`, `WardrobeFragment`,
`OutfitsFragment`, `ProfileFragment`, `SignInFragment` (renamed from `LoginFragment`).

New stub fragments, one per screen, package-grouped to match this codebase's existing
per-tab convention:

| Package | New fragments |
|---|---|
| `ui.auth` | `SplashFragment` (routing logic), `OnboardingFragment`, `BiometricFragment` |
| `ui.home` | `WeatherFragment` |
| `ui.wardrobe` | `ItemEditFragment`, `ColourMatcherFragment` (renamed), `LaundryFragment` |
| `ui.itemdetail` | `ItemDetailFragment` (wired to the pre-existing, previously-unused `ItemDetailViewModel`) |
| `ui.additem` | `CameraFragment`, `CutoutFragment`, `TagItemFragment` (renamed/moved from `ui.Capture`) |
| `ui.outfits` | `OutfitBuilderFragment`, `OutfitDetailFragment` |
| `ui.profile` | `StatsFragment`, `NotificationsFragment`, `SettingsFragment`, `LanguageFragment` |
| `ui.swap` | `SwapMarketFragment`, `ListingDetailFragment`, `MyListingsFragment`, `CreateListingFragment` |
| `ui.sheets` | `StubBottomSheet` (base), `FilterSheet`, `SortSheet`, `DayDetailSheet`, `PickOutfitSheet`, `ConfidenceSheet`, `ConfirmSheet` |

Nav arguments (declared in the graph, in `NavArgs.kt`): `itemId` (long — item detail/edit/colour
matcher/create-listing), `outfitId` (string — outfit detail/confidence sheet), `listingId`
(string — listing detail), `dateISO` (string — day-detail sheet).

Only the transitions that are structurally required to make the graph reachable/testable got
a real button wired up: Splash's auto-routing, Onboarding → Sign In → Biometric → Home, and
Camera → Cutout → Tag Item → Item Detail. Every other edge from the corrected flow (e.g.
Wardrobe → Item Detail, Outfits → Outfit Detail, Profile → Stats/Swap/Settings, etc.) is
declared as a real `<action>` in the graph so it's ready to fire once the real screen exists,
but has no button yet since the source screen is still a plain stub.

## 5. Not done / explicitly out of scope

- No pixel-accurate screens — every new fragment is a title+body stub, per your instruction.
- No Safe Args plugin added; nav arguments are plain `<argument>` tags read via
  `arguments`/`SavedStateHandle`, matching the existing `NavArgs` constant-object pattern (no
  Safe Args codegen was configured in `build.gradle.kts` before this change either).
- `settings.biometrics`-conditional routing (see §3) simplified to always show Biometric Unlock.
- `CatalogActivity` (the component-gallery dev tool) untouched — it's unrelated to app
  navigation.

## 6. Verification

`./gradlew assembleDebug` — **BUILD SUCCESSFUL**, zero errors (only pre-existing deprecation
warnings from `GoogleSignIn`/`GoogleSignInOptions`, unrelated to this change). This confirms
the graph, every new/renamed Kotlin file, and the manifest change all compile and resource-link
correctly. Not verified: on-device/emulator run (no emulator available in this environment) —
recommend a manual run through Splash → Onboarding → Sign In → Biometric → Home → tap the FAB →
Camera → Cutout → Tag Item → Item Detail, and a tab-switch + sign-out check, before merging.
