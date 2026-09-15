package com.example.runway.ui.outfits

import com.example.runway.domain.model.Outfit

// Screen state for the saved outfits list (IIE, 2026).

data class OutfitsUiState(
    val outfits: List<Outfit> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
