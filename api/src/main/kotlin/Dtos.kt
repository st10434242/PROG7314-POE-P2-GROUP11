package com.example

import com.google.cloud.Timestamp
import kotlinx.serialization.Serializable
import java.time.Instant

// Request and response DTOs for the Runway REST API (IIE, 2026; Tagliaferri, 2016).
// Serialisation is driven by annotations (Kotlin Foundation, 2023).
// One page of results plus the cursor needed to ask for the next (Google, 2026).
// Data classes are serialised with kotlinx.serialization (JetBrains, 2026).
@Serializable
data class PageResponse<T>(
    val data: List<T>,
    val nextCursor: String? = null,
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
)

@Serializable
data class ItemResponse(
    val id: String,
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val wearCount: Long = 0,
    // Computed on the server so every client shows the same number.
    val costPerWear: Double? = null,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
) {
    // Validation happens on the server even though the app validates too: the app can be bypassed by anyone with the API's URL and a token.
    fun validate() {
        if (name.isBlank()) throw ValidationException("name is required")
        if (name.length > MAX_NAME) throw ValidationException("name must be $MAX_NAME characters or fewer")
        if (category.isBlank()) throw ValidationException("category is required")
        if (purchasePrice != null && purchasePrice < 0) {
            throw ValidationException("purchasePrice cannot be negative")
        }
    }
}

// A partial update.
@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val category: String? = null,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val archived: Boolean? = null,
) {
    fun validate() {
        if (name != null && name.isBlank()) throw ValidationException("name cannot be blank")
        if (category != null && category.isBlank()) throw ValidationException("category cannot be blank")
        if (purchasePrice != null && purchasePrice < 0) {
            throw ValidationException("purchasePrice cannot be negative")
        }
        if (name == null && category == null && colour == null && brand == null &&
            size == null && purchasePrice == null && archived == null
        ) {
            throw ValidationException("Provide at least one field to update")
        }
    }
}

@Serializable
data class LogWearRequest(
    val outfitId: String? = null,
    // ISO-8601 instant.
    val wornOn: String? = null,
)

@Serializable
data class WearResponse(
    val id: String,
    val clothingItemId: String,
    val outfitId: String? = null,
    val wornOn: String? = null,
    val newWearCount: Long = 0,
)

@Serializable
data class OutfitItemDto(
    val id: String = "",
    val clothingItemId: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val zIndex: Long = 0,
)

@Serializable
data class OutfitResponse(
    val id: String,
    val name: String,
    val occasion: String? = null,
    val season: String? = null,
    val coverImagePath: String? = null,
    val items: List<OutfitItemDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class CreateOutfitRequest(
    val name: String,
    val occasion: String? = null,
    val season: String? = null,
    val coverImagePath: String? = null,
    val items: List<OutfitItemDto> = emptyList(),
) {
    fun validate() {
        if (name.isBlank()) throw ValidationException("name is required")
        if (items.any { it.clothingItemId.isBlank() }) {
            throw ValidationException("every outfit item needs a clothingItemId")
        }
    }
}

@Serializable
data class UserResponse(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class SettingsDto(
    val theme: String = "SYSTEM",
    val language: String = "en",
    val units: String = "METRIC",
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
) {
    fun validate() {
        if (theme !in setOf("LIGHT", "DARK", "SYSTEM")) {
            throw ValidationException("theme must be LIGHT, DARK or SYSTEM")
        }
        if (units !in setOf("METRIC", "IMPERIAL")) {
            throw ValidationException("units must be METRIC or IMPERIAL")
        }
    }
}

// Firestore timestamp to ISO-8601, for JSON.
fun Timestamp?.toIso(): String? = this?.toDate()?.toInstant()?.toString()

// ISO-8601 to Firestore timestamp, for storage.
fun String.toTimestamp(): Timestamp =
    try {
        Timestamp.ofTimeSecondsAndNanos(Instant.parse(this).epochSecond, Instant.parse(this).nano)
    } catch (e: Exception) {
        throw ValidationException("'$this' is not a valid ISO-8601 timestamp")
    }

// Cost per wear - the number the wardrobe screen is built around.
fun costPerWear(price: Double?, wearCount: Long): Double? =
    if (price == null || wearCount <= 0) null
    else Math.round((price / wearCount) * 100.0) / 100.0

private const val MAX_NAME = 120

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 11 September 2026].
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 11 September 2026].
Kotlin Foundation, 2023. Annotations. [online] Available at: <https://kotlinlang.org/docs/annotations.html> [Accessed 31 July 2023].
Tagliaferri, L., 2016. An Introduction to JSON. [online] Available at: <https://www.digitalocean.com/community/tutorials/an-introduction-to-json> [Accessed 31 July 2023].
*/
