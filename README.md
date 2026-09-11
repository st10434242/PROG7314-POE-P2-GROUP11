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

The repository holds two independent Gradle builds:

```
PROG7314-POE-P2-GROUP11/
├─ app/    the Android app     (Android Gradle Plugin)
└─ api/    the REST API        (plain JVM Kotlin, Ktor)
```

They share a Git history but nothing else - `api/` has its own wrapper, its own
`settings.gradle.kts` and its own dependencies. Open each in its own Android
Studio window.

```
app/src/main/java/com/example/runway/
  ui/
    components/     the design system: custom views + helpers
    catalog/        CatalogActivity - every component on one screen
    home/ wardrobe/ outfits/ profile/   one package per tab
    navigation/     nav-graph argument names
  domain/           plain Kotlin models + repository interfaces
  data/
    local/          Room entities, DAOs, database
    mapper/         entity <-> domain / DTO
    remote/api/     Retrofit interface, DTOs, auth interceptor
    repository/     repository implementations
  di/               AppContainer - manual dependency graph

api/src/main/kotlin/
  Application.kt    module wiring
  plugins           Serialization, Monitoring, ErrorHandling, Security
  FirebaseAdmin.kt  Firestore connection
  Documents.kt      Firestore document classes
  Dtos.kt           request/response shapes
  *Service.kt       Firestore queries and business rules
  *Routes.kt        HTTP endpoints
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

## Running the project

Two processes, started in this order. The API must be listening before the app
asks it for anything.

### Prerequisites

| Need | Where it comes from |
|---|---|
| JDK 21 | [Temurin 21](https://adoptium.net/temurin/releases/?version=21). Android Studio's bundled runtime is not always usable from a terminal. |
| Android SDK | Android Studio. `local.properties` is git-ignored - create it with `sdk.dir` if Studio has not. |
| `api/serviceAccountKey.json` | Firebase console → Project settings → Service accounts → **Generate new private key**. Never commit it. |
| `app/google-services.json` | Firebase console → Project settings → Your apps → Android → **Download**. |
| Your debug SHA-1 registered | `./gradlew signingReport`, then Firebase → Project settings → Your apps → **Add fingerprint**. |
| Google sign-in enabled | Firebase console → Authentication → Sign-in method → **Google**. |

Every developer has their own `debug.keystore`, so **each team member must add
their own SHA-1**. Without it Google Sign-In returns a null ID token and the app
reports "invalid account" with no further explanation.

### 1. Start the API

```powershell
cd api
.\run-api.ps1
```

`run-api.ps1` locates a working JDK, sets `JAVA_HOME` for you and starts the
server. Plain `./gradlew run` works too if your `JAVA_HOME` is already correct.

Wait for:

```
Application started in 3.4 seconds.
Responding at http://0.0.0.0:8080
```

**Gradle will sit at `83% EXECUTING` with a running timer and never reach 100%.
That is correct** - the `run` task only completes when the server exits. Reaching
100% means the server has died. Stop it with `Ctrl+C`, and leave the window open
while you use the app.

Two checks in a browser:

| URL | Expect |
|---|---|
| `http://localhost:8080/health` | `{"status":"ok","version":"1.0.1"}` |
| `http://localhost:8080/swagger` | The interactive endpoint list |

### 2. Run the app

Open the **repository root** in Android Studio - a separate window from `api/`.
Set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle
JDK** to Temurin 21 if the build complains about the JDK. Then Run on an
emulator running API 26 or higher.

The debug build points at `http://10.0.2.2:8080`, which is how the emulator
reaches the host machine's `localhost`. The release build points at the deployed
URL. Both are set as `API_BASE_URL` in `app/build.gradle.kts`; no code names a
host.

### 3. Confirm the two halves are talking

Sign in with Google, then filter Logcat by `OkHttp`:

```
--> GET http://10.0.2.2:8080/api/v1/items
<-- 200 OK http://10.0.2.2:8080/api/v1/items
{"data":[],"nextCursor":null}
```

The same request appears in the API's terminal as
`GET /api/v1/items -> 200 OK`. An empty `data` array is correct for a new
account.

| Symptom | Cause |
|---|---|
| `401` on every request | Google sign-in never reached Firebase - check the SHA-1 and that Google is enabled |
| `Failed to connect to /10.0.2.2:8080` | The API is not running, or its window was closed |
| `FAILED_PRECONDITION` in the API log | A Firestore index is missing - the log line contains a link that creates it |

## Testing the API with Swagger

The API serves its own interactive documentation at
`http://localhost:8080/swagger`, generated from
`api/src/main/resources/openapi/documentation.yaml`. Everything except
`/health` requires a Firebase ID token.

### Get a token

The quickest source is an email/password test user - no app required. Enable
**Email/Password** under Authentication → Sign-in method, add a user under
Authentication → Users, then:

```powershell
$key  = "<Web API key from Project settings → General>"
$body = @{ email='test@runway.local'; password='<password>'; returnSecureToken=$true } | ConvertTo-Json
$r = Invoke-RestMethod -Method Post -ContentType 'application/json' -Body $body `
  -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key"
$r.idToken | Set-Clipboard
```

In Swagger click **Authorize**, paste the token, **Close**. Paste the token
alone - Swagger adds the word `Bearer` itself. Tokens expire after an hour.

If the **Servers** dropdown at the top of the page is not on
`http://localhost:8080`, change it. Aiming at a deployed URL that does not exist
yet produces a bare "Failed to fetch" with no explanation.

### A full CRUD walk-through

| Step | Endpoint | Body | Expect |
|---|---|---|---|
| 1 | `POST /api/v1/items` | `{"name":"Black wool coat","category":"OUTERWEAR","purchasePrice":1299}` | **201** and a generated `id` |
| 2 | `GET /api/v1/items` | - | the item in `data` |
| 3 | `PATCH /api/v1/items/{id}` | `{"brand":"Levi's"}` | **200**, brand changed, nothing else touched |
| 4 | `POST /api/v1/items/{id}/wears` | `{}` | **201**, `newWearCount` increments |
| 5 | `GET /api/v1/items/{id}` | - | `costPerWear` = price ÷ wears |
| 6 | `DELETE /api/v1/items/{id}` | - | **204**, and gone from the list |
| 7 | Firestore console | - | the document is still there with `deleted: true` |

**Copy the `id`, never retype it.** Firestore ids mix `l`/`I`/`1` and `0`/`O`,
and a mistyped id returns a 404 that looks like a missing-record bug.

Step 7 is the point of the soft delete: a device that was offline when the
delete happened still learns about it on its next sync. A hard delete would
leave that device with an item that never goes away.

### Tests worth running before a demo

| Test | Expect |
|---|---|
| Any endpoint with no token | **401** |
| Blank `name`, or a negative `purchasePrice` | **400** `validation_failed` |
| A body missing a required field | **400** `malformed_request` |
| An unknown id | **404** `not_found` |
| **User B requesting user A's item id** | **404**, and A's item survives |

The last one is the one to be sure of. Sign in as a second user, take an item id
belonging to the first, and try to `GET` and `DELETE` it. Both must answer 404 -
not 403, which would confirm the id exists, and not the item itself.

Automated tests that need no Firestore connection:

```powershell
cd api
.\run-api.ps1 test
```

## Building

```bash
# from the repository root - the Android app
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
./gradlew signingReport        # your debug SHA-1

# from api/ - the REST API
./gradlew run                  # start the server
./gradlew test                 # unit tests
./gradlew buildFatJar          # a runnable jar for deployment
```

The app requires JDK 17+ and the Android SDK; the API requires JDK 21. `local.properties` is machine-specific and
git-ignored; create it with `sdk.dir` pointing at your SDK if Android Studio has
not already.
