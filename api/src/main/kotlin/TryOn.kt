package com.example

import kotlinx.serialization.Serializable
import java.util.Base64

// Virtual try-on: the user's photo wearing their own clothes (IIE, 2026).
// Images arrive as base64 from the app and are handed to whichever engine is
// configured, so nothing has to be stored in a bucket first.

@Serializable
data class TryOnGarmentDto(
    // Base64 of a PNG or JPEG, without a data: prefix.
    val image: String,
    val category: String,
    // What the garment is, in words. The model uses this as well as the picture.
    val description: String = "",
) {
    fun validate() {
        if (image.isBlank()) throw ValidationException("Every garment needs an image")
        if (image.length > MAX_IMAGE_CHARS) {
            throw ValidationException("Garment image is too large; send a smaller photo")
        }
        if (modelCategory() == null) {
            throw ValidationException(
                "category must be one of ${SUPPORTED_CATEGORIES.keys}. Shoes and accessories cannot be rendered."
            )
        }
    }

    // The model works in three garment families, which is fewer than the app has.
    fun modelCategory(): String? = SUPPORTED_CATEGORIES[category.uppercase()]
}

@Serializable
data class TryOnRequest(
    // Base64 of the user's full-body photo.
    val personImage: String,
    val garments: List<TryOnGarmentDto>,
) {
    fun validate() {
        if (personImage.isBlank()) throw ValidationException("personImage is required")
        if (personImage.length > MAX_IMAGE_CHARS) {
            throw ValidationException("The body photo is too large; send a smaller one")
        }
        if (garments.isEmpty()) throw ValidationException("Send at least one garment")
        // Each garment is a separate render, so the ceiling is a time and cost guard.
        if (garments.size > MAX_GARMENTS) {
            throw ValidationException("At most $MAX_GARMENTS garments can be rendered at once")
        }
        garments.forEach { it.validate() }
    }
}

@Serializable
data class TryOnResponse(
    // Base64 PNG of the finished render, ready for the app to show and cache.
    val image: String,
    // How many renders this took: one per garment.
    val renders: Int,
    val elapsedMs: Long,
    // Which engine produced it, so a slow or odd result can be explained.
    val engine: String = "",
)

// TOP and OUTERWEAR both go on the upper body as far as the model is concerned.
private val SUPPORTED_CATEGORIES = mapOf(
    "TOP" to "upper_body",
    "OUTERWEAR" to "upper_body",
    "BOTTOM" to "lower_body",
    "DRESS" to "dresses",
)

// Roughly a 1.5 MB image once base64 has added its third.
private const val MAX_IMAGE_CHARS = 2_000_000
private const val MAX_GARMENTS = 2

class TryOnService(
    private val engine: TryOnEngine = TryOnEngine.fromEnvironment(),
) {

    val isConfigured: Boolean get() = engine.isConfigured

    // Garments are applied one after another: the model dresses a person in one
    // item per run, so a top and a trouser is two runs, the second wearing the
    // output of the first.
    suspend fun render(body: TryOnRequest): TryOnResponse {
        body.validate()

        val started = System.currentTimeMillis()
        val decoder = Base64.getDecoder()
        var person = decoder.decode(body.personImage)

        body.garments.forEach { garment ->
            person = engine.dress(
                person = person,
                garment = decoder.decode(garment.image),
                description = garment.description.ifBlank { garment.category.lowercase() },
                category = garment.modelCategory()!!,
            )
        }

        return TryOnResponse(
            image = Base64.getEncoder().encodeToString(person),
            renders = body.garments.size,
            elapsedMs = System.currentTimeMillis() - started,
            engine = engine.name,
        )
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
