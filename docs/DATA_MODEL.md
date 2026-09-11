# Runway data model

**Ticket:** SCRUM-103 — create entities / data models / tables
**Database:** Cloud Firestore (Native mode)
**Owner of this document:** Group 11

Firestore is a document database, so there are no tables and no schema enforced
by the server. The structure below is therefore a contract the code keeps rather
than one the database checks, which is why it is written down here and mirrored
by the classes in `api/src/main/kotlin/Documents.kt` (Google, 2026).

---

## Conventions applied to every collection

| Field | Type | Why it exists |
|---|---|---|
| `ownerUid` | string | The uid from the verified Firebase ID token. Every query filters on it; it is what prevents one user reading another's wardrobe. |
| `createdAt` | timestamp | When the document was first written. |
| `updatedAt` | timestamp | When it last changed. Drives the incremental sync in SCRUM-82. |
| `deleted` | boolean | Soft-delete flag. A device that was offline cannot discover a document that simply vanished. |

Two naming rules, both of which exist to avoid silent bugs:

- **Booleans are not named `isSomething`.** Kotlin compiles `isDeleted` to a
  getter named `isDeleted()`, which the Admin SDK maps back to a field called
  `deleted` — so the document written and the document read disagree. The
  fields are named `deleted` and `archived` throughout.
- **Timestamps are Firestore `Timestamp` values, never strings.** Only a real
  timestamp can be sorted and range-queried, which `?updatedSince=` depends on
  (Google, 2026).

## Root collection or subcollection?

One rule decides every case: **if the data is ever queried without knowing its
parent, it must be a root collection** (Google, 2026).

- Outfit placements are only read while viewing one outfit → subcollection.
- Clothing items are filtered by category across a whole wardrobe → root.
- Listings must be visible to *other* users → root, never nested under an owner.

---

## Collections

### `users/{uid}`

Document id is the Firebase uid, so the profile is a single-document read.

| Field | Type | Notes |
|---|---|---|
| `uid` | string | Matches the document id |
| `displayName` | string | |
| `email` | string | |
| `photoUrl` | string? | From the Google account |
| `createdAt` / `updatedAt` | timestamp | |

### `users/{uid}/settings/preferences`

A fixed document id rather than a generated one, so reading settings is a fetch
rather than a query.

| Field | Type | Allowed values |
|---|---|---|
| `theme` | string | `LIGHT`, `DARK`, `SYSTEM` |
| `language` | string | ISO 639-1, e.g. `en`, `af`, `zu` |
| `units` | string | `METRIC`, `IMPERIAL` |
| `notificationsEnabled` | boolean | |
| `biometricEnabled` | boolean | |
| `updatedAt` | timestamp | |

### `clothingItems/{itemId}`

| Field | Type | Notes |
|---|---|---|
| `id` | string | Copy of the document id, so the object is self-describing |
| `ownerUid` | string | |
| `name` | string | Required, ≤ 120 characters |
| `category` | string | `TOP`, `BOTTOM`, `OUTERWEAR`, `SHOES`, `ACCESSORY` |
| `colour` / `brand` / `size` | string? | |
| `purchasePrice` | number? | ZAR. Never negative |
| `purchaseDate` | timestamp? | |
| `wearCount` | integer | Incremented server-side by `FieldValue.increment` |
| `archived` | boolean | Out of rotation but not deleted |
| `deleted` | boolean | |
| `createdAt` / `updatedAt` | timestamp | |

`costPerWear` is **not stored**. It is `purchasePrice / wearCount`, computed by
the API on every response so the two values can never drift apart.

### `clothingItems/{itemId}/images/{imageId}`

| Field | Type |
|---|---|
| `storagePath` | string |
| `primary` | boolean |
| `width` / `height` | integer |
| `uploadedAt` | timestamp |

### `outfits/{outfitId}`

| Field | Type |
|---|---|
| `ownerUid` | string |
| `name` | string |
| `occasion` / `season` | string? |
| `coverImagePath` | string? |
| `deleted` | boolean |
| `createdAt` / `updatedAt` | timestamp |

### `outfits/{outfitId}/items/{id}`

The position of one garment on the outfit canvas.

| Field | Type | Notes |
|---|---|---|
| `clothingItemId` | string | Reference to `clothingItems` |
| `x` / `y` | number | Canvas coordinates |
| `scale` / `rotation` | number | |
| `zIndex` | integer | Layer order |

### `outfitPlans/{planId}`

| Field | Type | Notes |
|---|---|---|
| `ownerUid` | string | |
| `date` | timestamp | The day being planned |
| `outfitId` | string | |
| `weatherSummary` | string? | Cached at plan time |
| `status` | string | `PLANNED`, `WORN`, `SKIPPED` |

### `wearHistory/{entryId}`

| Field | Type |
|---|---|
| `ownerUid` | string |
| `clothingItemId` | string |
| `outfitId` | string? |
| `wornOn` | timestamp |

### `confidenceRatings/{id}`

| Field | Type | Notes |
|---|---|---|
| `ownerUid` | string | |
| `outfitId` | string | |
| `score` | integer | 1–5 |
| `note` | string? | |
| `ratedAt` | timestamp | |

### `listings/{listingId}`

| Field | Type | Notes |
|---|---|---|
| `ownerUid` | string | |
| `clothingItemId` | string | |
| `type` | string | `SWAP`, `DONATE` |
| `status` | string | `OPEN`, `RESERVED`, `CLOSED` |
| `description` | string? | |

### `listingRequests/{requestId}`

| Field | Type | Notes |
|---|---|---|
| `listingId` | string | |
| `listingOwnerUid` | string | Denormalised so the owner's requests can be listed without a join |
| `requesterUid` | string | |
| `message` | string? | |
| `status` | string | `PENDING`, `ACCEPTED`, `DECLINED` |

---

## Indexes

Firestore indexes single fields automatically, but any query that filters on one
field and orders by another needs the combination declared in advance, otherwise
it fails at run time with `FAILED_PRECONDITION` (Google, 2026). The declarations
live in `api/firestore.indexes.json`.

When a query fails, the error message contains a console link that creates the
exact index. Click it, then copy the definition back into that file so it stays
in source control.

## Security rules

`api/firestore.rules` denies every direct client request. The Admin SDK used by
the API is not subject to security rules, so the API keeps working while
everything else is locked out — which forces all traffic through the endpoints
where ownership is checked (Google, 2026; Google, 2026).

---

## Reference List

IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.

Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 11 September 2026].

Google, 2026. Choose a data structure. [online] Available at: <https://firebase.google.com/docs/firestore/manage-data/structure-data> [Accessed 11 September 2026].

Google, 2026. Get started with Cloud Firestore Security Rules. [online] Available at: <https://firebase.google.com/docs/firestore/security/get-started> [Accessed 11 September 2026].

Google, 2026. Index types in Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/index-overview> [Accessed 11 September 2026].

Google, 2026. Supported data types in Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/manage-data/data-types> [Accessed 11 September 2026].

## Reference List

IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.

Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 11 September 2026].

## Reference List

IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.

Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 11 September 2026].
