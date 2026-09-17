package com.example.runway.ui.wardrobe

import com.example.runway.domain.model.ColourMatch
import com.example.runway.domain.model.Item
import com.example.runway.domain.model.ItemColour

data class ColourMatcherUiState(
    val reference: Item? = null,
    val referenceColour: ItemColour? = null,
    val matches: List<ColourMatch> = emptyList(),
    val isLoading: Boolean = true
)