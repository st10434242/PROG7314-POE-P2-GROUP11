package com.example

import com.google.cloud.Timestamp
import kotlinx.serialization.Serializable

// Firestore document and DTOs for the user's body profile (IIE, 2026).
// Stored at users/{uid}/model/profile, one fixed document per user (Google, 2026).
// The model itself is the user's photo and never leaves their device; what is kept
// here are the measurements a try-on render needs.

data class ModelProfileDocument(
    var heightCm: Long = 170,
    var weightKg: Long? = null,
    var topSize: String? = null,
    var bottomSize: String? = null,
    var shoeSizeEu: Long? = null,
    // Worked out from the photo by the app, kept so other devices can use it.
    var bodyType: String? = null,
    var bodyShape: String? = null,
    var updatedAt: Timestamp? = null,
)

@Serializable
data class ModelProfileDto(
    val heightCm: Int = 170,
    val weightKg: Int? = null,
    val topSize: String? = null,
    val bottomSize: String? = null,
    val shoeSizeEu: Int? = null,
    val bodyType: String? = null,
    val bodyShape: String? = null,
    val updatedAt: String? = null,
) {
    // Validated on the server as well as the app: the app can be bypassed.
    fun validate() {
        if (heightCm !in MIN_HEIGHT..MAX_HEIGHT) {
            throw ValidationException("heightCm must be between $MIN_HEIGHT and $MAX_HEIGHT")
        }
        if (weightKg != null && weightKg !in MIN_WEIGHT..MAX_WEIGHT) {
            throw ValidationException("weightKg must be between $MIN_WEIGHT and $MAX_WEIGHT")
        }
        if (shoeSizeEu != null && shoeSizeEu !in MIN_SHOE..MAX_SHOE) {
            throw ValidationException("shoeSizeEu must be between $MIN_SHOE and $MAX_SHOE")
        }
        if (topSize != null && topSize !in SIZES) throw ValidationException("topSize must be one of $SIZES")
        if (bottomSize != null && bottomSize !in SIZES) throw ValidationException("bottomSize must be one of $SIZES")
        if (bodyType != null && bodyType !in BODY_TYPES) throw ValidationException("bodyType must be one of $BODY_TYPES")
        if (bodyShape != null && bodyShape !in BODY_SHAPES) throw ValidationException("bodyShape must be one of $BODY_SHAPES")
    }

    companion object {
        val SIZES = setOf("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL")
        val BODY_TYPES = setOf("SLIM", "AVERAGE", "ATHLETIC", "FULL")
        val BODY_SHAPES = setOf("RECTANGLE", "TRIANGLE", "INVERTED_TRIANGLE", "HOURGLASS", "OVAL")
        const val MIN_HEIGHT = 120
        const val MAX_HEIGHT = 220
        const val MIN_WEIGHT = 30
        const val MAX_WEIGHT = 250
        const val MIN_SHOE = 33
        const val MAX_SHOE = 50
    }
}

fun ModelProfileDocument.toDto(): ModelProfileDto = ModelProfileDto(
    heightCm = heightCm.toInt(),
    weightKg = weightKg?.toInt(),
    topSize = topSize,
    bottomSize = bottomSize,
    shoeSizeEu = shoeSizeEu?.toInt(),
    bodyType = bodyType,
    bodyShape = bodyShape,
    updatedAt = updatedAt.toIso(),
)

fun ModelProfileDto.toDocument(): ModelProfileDocument = ModelProfileDocument(
    heightCm = heightCm.toLong(),
    weightKg = weightKg?.toLong(),
    topSize = topSize,
    bottomSize = bottomSize,
    shoeSizeEu = shoeSizeEu?.toLong(),
    bodyType = bodyType,
    bodyShape = bodyShape,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2026. Add the Firebase Admin SDK to your server. [online] Available at: <https://firebase.google.com/docs/admin/setup> [Accessed 15 September 2026].
*/
