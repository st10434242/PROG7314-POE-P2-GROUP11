package com.example.runway.domain.model

// The user's body profile (IIE, 2026).
// The model is the user's own photo, so nothing here describes a drawn figure.
// These are the measurements a try-on needs, plus the two things pose detection
// can work out from the photo on its own.

// Overall build, read from the photo rather than chosen.
enum class BodyType {
    SLIM,
    AVERAGE,
    ATHLETIC,
    FULL;

    companion object {
        fun from(value: String?): BodyType? = entries.firstOrNull { it.name == value }
    }
}

// Silhouette, read from the photo rather than chosen.
enum class BodyShape {
    RECTANGLE,
    TRIANGLE,
    INVERTED_TRIANGLE,
    HOURGLASS,
    OVAL;

    companion object {
        fun from(value: String?): BodyShape? = entries.firstOrNull { it.name == value }
    }
}

data class ModelProfile(
    val heightCm: Int = DEFAULT_HEIGHT_CM,
    val weightKg: Int? = null,
    val topSize: ClothingSize? = null,
    val bottomSize: ClothingSize? = null,
    val shoeSizeEu: Int? = null,
    // Detected from the photo. Shown as information, not offered as a choice.
    val bodyType: BodyType? = null,
    val bodyShape: BodyShape? = null,
    val updatedAt: String? = null,
) {
    // A try-on needs the body and at least one size to reason about.
    val isComplete: Boolean
        get() = weightKg != null && (topSize != null || bottomSize != null)

    companion object {
        const val MIN_HEIGHT_CM = 120
        const val MAX_HEIGHT_CM = 220
        const val DEFAULT_HEIGHT_CM = 170

        const val MIN_WEIGHT_KG = 30
        const val MAX_WEIGHT_KG = 250
        const val DEFAULT_WEIGHT_KG = 70

        const val MIN_SHOE_EU = 33
        const val MAX_SHOE_EU = 50
        const val DEFAULT_SHOE_EU = 41
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
