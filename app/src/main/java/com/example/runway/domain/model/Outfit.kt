package com.example.runway.domain.model

// A saved outfit: the garments worn together, and the render of them (IIE, 2026).

data class Outfit(
    val id: String = "",
    val name: String,
    // The garments in the outfit, in the order they were put on.
    val itemIds: List<String> = emptyList(),
    // Where the try-on render is stored on this device. Outfits sync through the
    // API but the picture does not, so another device shows the outfit without it.
    val renderPath: String? = null,
    val updatedAt: String? = null,
) {
    val garmentCount: Int get() = itemIds.size
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
