package com.example.runway.ui.outfits

import android.graphics.Bitmap
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.Outfit

// Screen state for one saved outfit (IIE, 2026).

data class OutfitDetailUiState(
    val outfit: Outfit? = null,
    // The garments in the outfit, resolved from the wardrobe so they have names.
    val garments: List<Item> = emptyList(),
    val render: Bitmap? = null,
    val name: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedAt: Long? = null,
    val deleted: Boolean = false,
    val errorMessage: String? = null,
) {
    // Only offer to save when there is something to save.
    val hasChanges: Boolean
        get() = outfit != null &&
            (name.trim() != outfit.name || garments.map { it.id } != outfit.itemIds)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
