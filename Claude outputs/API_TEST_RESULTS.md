# Runway REST API - test results

PROG7314 POE Part 2, Group 11.
Run against the API on `http://localhost:8080` with Firebase Admin talking to the
live `runway-part2-group11` Firestore project, 15 September 2026.

Seventy-three checks were executed across seven areas. Two defects were found;
both were fixed and re-tested. Every check passes in the final state.

Requests were issued with two separate Firebase accounts (referred to below as
user A and user B) so that ownership rules could be proved rather than assumed.
All test documents were removed afterwards.

---

## 1. Virtual model endpoints (SCRUM-84, SCRUM-85, SCRUM-86)

`GET` and `PUT /api/v1/users/me/model`.

| # | Check | Result |
|---|---|---|
| M1 | `GET` without a token returns 401 | Pass |
| M2 | `GET` with an invalid token returns 401 | Pass |
| M3 | A user who has never saved a model receives the defaults, not a 404 | Pass |
| M4 | A valid `PUT` returns 200 and echoes the saved profile | Pass |
| M5 | A saved profile carries an `updatedAt` timestamp | Pass |
| M6 | The saved profile survives a fresh `GET` | Pass |
| M7 | User B's model is unchanged by user A's write | Pass |

Returning defaults rather than 404 (M3) is deliberate: the model screen always has
something to draw, so it never has to special-case a first run.

## 2. Model validation

The app validates these too, but the API can be called without the app, so the
server is the boundary that matters (OWASP, 2026).

| # | Check | Result |
|---|---|---|
| V1 | Unknown `bodyType` rejected with 400 | Pass |
| V2 | Unknown `bodyShape` rejected with 400 | Pass |
| V3 | Unknown `skinTone` rejected with 400 | Pass |
| V4 | A lower-case enum value is rejected | Pass |
| V5 | `heightCm` of 119 (below the minimum) rejected | Pass |
| V6 | `heightCm` of 221 (above the maximum) rejected | Pass |
| V7 | `heightCm` of 120 (the minimum) accepted | Pass |
| V8 | `heightCm` of 220 (the maximum) accepted | Pass |
| V9 | Malformed JSON returns 400 `malformed_request`, not 500 | Pass |
| V10 | A partial body fills the remaining fields with defaults | Pass |
| V11 | A string where `heightCm` expects a number returns 400 | Pass |
| V12 | `PUT` without a token returns 401 | Pass |

V5 to V8 test both sides of each boundary rather than only the middle of the
range, which is where off-by-one errors hide.

## 3. Clothing items, wear limits and settings

| # | Check | Result |
|---|---|---|
| C1 | An item created with `wearLimit: 2` keeps that limit | Pass |
| C2 | An item created without one inherits the settings default | Pass |
| C3 | `GET /api/v1/items/{id}` | Pass |
| C4 | `PATCH /api/v1/items/{id}` | Pass |
| C5 | First wear raises the count to 1 | Pass |
| C6 | Second wear raises the count to 2 | Pass |
| C7 | `needsWash` turns true when the count reaches the limit | Pass |
| C8 | `costPerWear` of an R400 item worn twice is 200.00 | Pass |
| C9 | `POST /api/v1/outfits` | Pass |
| C10 | `GET /api/v1/outfits/{id}` returns both placements | Pass |
| C11 | `PATCH /api/v1/outfits/{id}` | Pass |
| C12 | `POST /api/v1/outfits/{id}/wears` logs a wear per garment | Pass |
| C13 | `GET /api/v1/wardrobe/summary` | **Failed, fixed - see defect 1** |
| C14 | `GET /api/v1/users/me` | Pass |
| C15 | `GET /api/v1/users/me/settings` | Pass |
| C16 | `PUT /api/v1/users/me/settings` | Pass |
| C17 | Changing `defaultWearLimit` changes what the next new item inherits | Pass |

C2, C7 and C17 together prove the whole wear-limit chain, from the Firestore
document through the service to the response.

## 4. Ownership and input validation

Both users hold valid tokens, so these test authorisation rather than authentication.

| # | Check | Result |
|---|---|---|
| O1 | User B cannot read user A's item (404) | Pass |
| O2 | User B cannot edit user A's item (404) | Pass |
| O3 | User B cannot delete user A's item (404) | Pass |
| O4 | User B cannot log a wear against user A's item (404) | Pass |
| O5 | User B cannot read user A's outfit (404) | Pass |
| O6 | User B's item list contains none of user A's items | Pass |
| O7 | User A's item is unchanged after all of the above | Pass |
| O8 | A blank name is rejected with 400 | Pass |
| O9 | A negative purchase price is rejected | Pass |
| O10 | A `wearLimit` of 0 is rejected | Pass |
| O11 | A `wearLimit` of 61 is rejected | Pass |
| O12 | An empty `PATCH` body is rejected | Pass |
| O13 | An unknown id returns 404 | Pass |

A missing resource and someone else's resource both return 404. A 403 would
confirm that the id exists, which tells an attacker something (OWASP, 2026).

## 5. Wardrobe summary, after defect 1 was fixed

| # | Check | Result |
|---|---|---|
| S1 | `GET /api/v1/wardrobe/summary` returns 200 | Pass |
| S2 | Item and outfit counts match the fixtures | Pass |
| S3 | `totalValue` includes the priced item | Pass |
| S4 | `totalWears` is counted | Pass |
| S5 | `needsWashCount` counts the item at its limit | Pass |
| S6 | Without a token, 401 | Pass |
| S7 | User B's summary counts only user B's wardrobe | Pass |
| S8 | The saved model survived an API restart | Pass |

## 6. Outfit list, after defect 2 was fixed

| # | Check | Result |
|---|---|---|
| L1 | `GET /api/v1/outfits` returns 200 | Pass |
| L2 | The list contains the expected outfit | Pass |
| L3 | Each entry carries its item placements | Pass |
| L4 | Without a token, 401 | Pass |
| L5 | User B sees none of user A's outfits | Pass |
| L6 | `limit=2` returns two rows and a cursor | Pass |
| L7 | The cursor returns the next page with no overlap | Pass |
| L8 | Results are ordered newest first | Pass |

## 7. Incremental sync

These are the contract the Android offline repository depends on.

| # | Check | Result |
|---|---|---|
| I1 | `category` filter returns only that category | Pass |
| I2 | `updatedSince` returns the items changed since the mark | Pass |
| I3 | An `updatedSince` in the future returns nothing | Pass |
| I4 | A soft-deleted item still reaches an incremental sync | Pass |
| I5 | A first load hides that same deleted item | Pass |
| I6 | A malformed `updatedSince` returns 400, not 500 | Pass |
| I7 | `limit=1` returns a cursor | Pass |

I4 and I5 are the pair that matters. An incremental sync needs deletions so the
app can remove them locally; a first load has nothing to reconcile, so removed
items are hidden.

---

## Defects found and fixed

### Defect 1 - the wardrobe summary endpoint was never routed

`GET /api/v1/wardrobe/summary` returned 404. `ItemService.summary()` was written,
`WardrobeSummaryResponse` existed, and the endpoint was described in the OpenAPI
document, but no route was registered for it, so nothing reached the service.
The Android client already declares `getWardrobeSummary()`, so any screen calling
it would have failed.

Fixed by registering the route in `ItemRoutes.kt`, inside the same authenticated
block as the item routes. Re-tested as section 5.

### Defect 2 - the outfit list query had no Firestore index

`GET /api/v1/outfits` returned 500. Firestore requires a composite index for a
query that combines equality filters with an order-by on another field, and the
outfit list filters on `ownerUid` and `deleted` while ordering by `updatedAt`
(Google, 2026). The index was already declared in `api/firestore.indexes.json`
but had never been created in the project, because no code path had listed
outfits until this test run.

Fixed by creating the index from the link in the `FAILED_PRECONDITION` error.
Re-tested as section 6.

Both defects were of the same kind: code that existed but had never been
exercised. Fetching a single outfit worked throughout, which is why the gap
survived earlier testing.

## Also changed

The OpenAPI document did not describe the model endpoints at all. Added
`/api/v1/users/me/model` with a `ModelProfile` schema and a `Model` tag, so
Swagger UI now documents the whole API surface.

## Not covered here

- The unit tests in `ServerTest.kt`, which run through Gradle.
- The on-device tests in `MODEL_FEATURE_TESTS.md`: the selection screen, the
  height slider and the layered preview all need the emulator.
- The Android side of sync. Section 7 proves the API's half of the contract;
  the repository's behaviour when the device is offline is tested on the device.

## Reference List

Google, 2026. Index types in Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/index-overview> [Accessed 15 September 2026].
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
OWASP, 2026. Broken access control. [online] Available at: <https://owasp.org/Top10/A01_2021-Broken_Access_Control/> [Accessed 15 September 2026].
