package com.example.runway.data.remote.api

import kotlinx.serialization.Serializable

// Network DTO for the user's body profile (IIE, 2026; Tagliaferri, 2016).
// Data classes are serialised with kotlinx.serialization (JetBrains, 2026).

// Enums and sizes cross the wire as strings so an unknown value from a newer app
// version falls back to null instead of failing to parse.
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
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 15 September 2026].
Tagliaferri, L., 2016. An Introduction to JSON. [online] Available at: <https://www.digitalocean.com/community/tutorials/an-introduction-to-json> [Accessed 31 July 2023].
*/
