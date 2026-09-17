package com.example.runway.ui.wardrobe

import com.example.runway.domain.model.Item

data class ColourMatcherUiState(
    val reference: Item? = null,
    val isLoading: Boolean = true
)