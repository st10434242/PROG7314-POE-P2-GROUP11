# Virtual model — test plan

**Tickets:** SCRUM-84, 85, 86, 87, 118, 88
**Feature:** choose a model, save it, and try wardrobe items on it

Work down the list. Each test assumes the ones above it passed, so the first
failure tells you where the problem is instead of leaving you guessing across
the whole feature. Capture the screenshots named in the last section as you go —
they are the evidence for SCRUM-88.

---

## Before you start

| # | Step | Expect |
|---|---|---|
| 0.1 | `cd api` then `.\run-api.ps1` | `Responding at http://0.0.0.0:8080`. The model endpoints only exist after a restart. |
| 0.2 | Browser → `http://localhost:8080/swagger` | `GET` and `PUT /api/v1/users/me/model` appear in the list |
| 0.3 | `.\gradlew assembleDebug` from the repository root | `BUILD SUCCESSFUL` |
| 0.4 | Add an entry point | `ProfileFragment` must call `findNavController().navigate(R.id.action_profile_to_model)`. Without it the screen exists but cannot be reached. |
| 0.5 | Wardrobe has items | Create two or three through Swagger or the app, with categories `TOP`, `BOTTOM` and `OUTERWEAR`, each with a `colour` such as `black` or `navy`. Nothing can be tried on otherwise. |

---

## 1. Model selection — SCRUM-84

| # | Step | Expect |
|---|---|---|
| 1.1 | Open the model screen | A figure is drawn, with Body shape, Build, Skin tone and Height controls below it |
| 1.2 | Tap each **Body shape** chip in turn | The silhouette changes: rectangle, triangle, inverted triangle, hourglass, oval are visibly different shapes |
| 1.3 | Tap a second chip in the same group | The first deselects. Exactly one shape is ever selected |
| 1.4 | Rotate the device | The selection survives; the figure redraws at the new size |

**Pass:** every option is reachable, the drawing responds immediately, and no
two options in a group can be selected at once.

---

## 2. The four attributes — SCRUM-85

| # | Step | Expect |
|---|---|---|
| 2.1 | Change **Build** from Slim to Full | The figure gets visibly wider. Height does not change |
| 2.2 | Change **Skin tone** across all six | The figure's colour changes; the garments on it do not |
| 2.3 | Drag **Height** from 120 to 220 | The figure stretches vertically. The label reads the current value in cm |
| 2.4 | Set Height to an extreme and release | The value stops at 120 and 220 — the slider cannot exceed the range |
| 2.5 | Combine: Full build, Deep tone, 190 cm, Hourglass | All four apply at once and none cancels another out |

**Pass:** all four attributes are independently adjustable and visible in the
drawing at the same time.

**Worth explaining in the demo:** there are five figure drawables in total, not
one per combination. Skin tone is applied as a tint, build scales the width and
height scales the height — so four attributes produce 4 x 5 x 6 x 101 possible
models from five files.

---

## 3. Saving the model — SCRUM-86

| # | Step | Expect |
|---|---|---|
| 3.1 | Set a distinctive model (say Oval, Athletic, Olive, 155 cm) and press **Save model** | The button reads "Saving…", then a "Model saved" toast |
| 3.2 | Watch the API terminal | `PUT /api/v1/users/me/model -> 200 OK` |
| 3.3 | Firebase console → Firestore → `users/{your uid}/model/profile` | A document with your four values and an `updatedAt` |
| 3.4 | Leave the screen and come back | Your model is still there |
| 3.5 | Force-stop the app, reopen, return to the screen | Still there — it loads from the local copy immediately, then refreshes from the API |
| 3.6 | **Uninstall the app, reinstall, sign in as the same user** | The model returns. This proves it is stored on the server, not just on the device |
| 3.7 | Sign in as a different user | A default model, not the first user's |

**Pass:** 3.6 is the one that matters. Anything less only proves local storage.

---

## 4. Wardrobe on the model — SCRUM-87

| # | Step | Expect |
|---|---|---|
| 4.1 | Look at **Try on from your wardrobe** | A chip per wardrobe item |
| 4.2 | Tap a `TOP` | The garment appears on the figure, tinted with that item's colour |
| 4.3 | Tap a `BOTTOM` | Both are now on, correctly positioned |
| 4.4 | Tap a **second** `TOP` | The first top comes off. Only one garment per category |
| 4.5 | Tap an `OUTERWEAR` item | It draws **over** the top, not under it |
| 4.6 | Tap a garment already on | It comes off |
| 4.7 | Press **Take everything off** | The figure is bare; chips all deselect |
| 4.8 | Change body shape while dressed | The garments stay aligned to the new silhouette |
| 4.9 | Empty wardrobe (new account) | The empty state explains how to add items — no crash, no blank space |

**Pass:** 4.5 and 4.8 are the interesting ones. Layer order is deliberate
(bottom → top → shoes → outerwear) and garments share the figure's bounds, which
is why they stay aligned at any size.

---

## 5. The combination on screen — SCRUM-118

| # | Step | Expect |
|---|---|---|
| 5.1 | Dress the model in a top, a bottom and outerwear | All three visible together in the right order |
| 5.2 | Read the line under the figure | "Wearing" followed by the names of the garments on the model |
| 5.3 | Remove one | The line updates immediately |
| 5.4 | Remove all | "Nothing on the model yet" |

**Pass:** the summary text always matches what is drawn.

---

## 6. Confirming it is really the API — Swagger

The screen could be lying to you with cached data. This proves it is not.

| # | Step | Expect |
|---|---|---|
| 6.1 | In Swagger, authorise with a token for the same account | Padlocks close |
| 6.2 | `GET /api/v1/users/me/model` | The values you saved in test 3 |
| 6.3 | `PUT` a different profile through Swagger | 200, with the new values |
| 6.4 | Force-stop and reopen the app | The app shows the profile you set in Swagger |
| 6.5 | `PUT` an invalid body, e.g. `{"heightCm": 500}` | **400** `validation_failed`, and the message names the field |
| 6.6 | `PUT` with `"bodyShape": "SQUARE"` | **400** — unknown values are rejected server-side, not silently stored |

**Pass:** 6.4 shows the app reading the server rather than its own cache; 6.5 and
6.6 show the API validating independently of the app.

---

## 7. When things go wrong

| # | Step | Expect |
|---|---|---|
| 7.1 | Stop the API, open the model screen | The last saved model still draws from the local copy |
| 7.2 | Press Save with the API stopped | An error message, not a crash |
| 7.3 | Restart the API and press Save | It succeeds |
| 7.4 | Emulator in aeroplane mode | The screen still works; only saving fails |

**Pass:** being offline is a normal state, not an error state.

---

## Evidence to capture — SCRUM-88

Five screenshots, and they cover every ticket in this group:

1. **The selection screen** with all four control groups visible — SCRUM-84, 85
2. **Two models side by side** (different shape, build, tone, height) — SCRUM-85
3. **The model dressed** in a top, bottom and outerwear, with the "Wearing" line — SCRUM-87, 118
4. **The Firestore document** at `users/{uid}/model/profile` — SCRUM-86
5. **The API terminal** showing `PUT /api/v1/users/me/model -> 200 OK` — SCRUM-86

For the live demonstration, the sequence worth performing is test 3.6: set a
model, uninstall the app, reinstall, sign in, and show it returning from the
server. It demonstrates the model, the saving and the API in one action.

---

## Reference List

IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.

Android Open Source Project, 2020c. Fragments. [online] Available at: <https://developer.android.com/guide/components/fragments> [Accessed 31 July 2023].

Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
