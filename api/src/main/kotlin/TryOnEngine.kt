package com.example

// The thing that actually renders a person wearing a garment (IIE, 2026).
// Two of these exist: a free Hugging Face Space and a paid Replicate model. The
// rest of the API does not care which, so a provider can be swapped by setting
// one environment variable instead of editing code.

interface TryOnEngine {

    // For the error message when nothing is set up.
    val name: String

    val isConfigured: Boolean

    // Puts one garment on one person and returns the finished image. Bytes in and
    // bytes out, so garments can be chained: the result of one call is the person
    // for the next.
    suspend fun dress(
        person: ByteArray,
        garment: ByteArray,
        description: String,
        category: String,
    ): ByteArray

    fun close()

    companion object {
        // Defaults to the free Space, because that is the one that needs no card.
        fun fromEnvironment(): TryOnEngine =
            when (System.getenv(PROVIDER_ENV)?.lowercase()?.trim()) {
                "replicate" -> ReplicateTryOn()
                else -> HuggingFaceTryOn()
            }

        const val PROVIDER_ENV = "TRYON_PROVIDER"
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
