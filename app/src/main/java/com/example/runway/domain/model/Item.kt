package com.example.runway.domain.model

// Domain model for a wardrobe garment (IIE, 2026).

// A garment in the user's wardrobe - the model everything above the data layer reasons about.
data class Item(
    val id: String = "",
    val name: String,
    val category: String,
    val colour: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val purchasePrice: Double? = null,
    val wearCount: Long = 0,
    // purchasePrice / wearCount.
    val costPerWear: Double? = null,
    val archived: Boolean = false,
    // Epoch milliseconds of the last change, local or remote.
    val updatedAt: Long = 0L,
    // True when this item has local changes the server has not accepted yet.
    val pendingSync: Boolean = false,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
