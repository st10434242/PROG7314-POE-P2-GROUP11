package com.example.runway.data.remote.api

import kotlinx.serialization.Serializable

// Network DTOs for the virtual try-on (IIE, 2026; Tagliaferri, 2016).
// Images travel as base64 so no bucket is needed between the phone and the model.

@Serializable
data class TryOnGarmentDto(
    val image: String,
    val category: String,
    val description: String = "",
)

@Serializable
data class TryOnRequestDto(
    val personImage: String,
    val garments: List<TryOnGarmentDto>,
)

@Serializable
data class TryOnResponseDto(
    val image: String,
    val renders: Int = 0,
    val elapsedMs: Long = 0,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Tagliaferri, L., 2016. An Introduction to JSON. [online] Available at: <https://www.digitalocean.com/community/tutorials/an-introduction-to-json> [Accessed 31 July 2023].
*/
