package com.example.runway.ui.items

import com.example.runway.domain.model.Item

/**
 * One screen, one state class. Exposing separate flows for loading, error and
 * data lets the UI observe combinations that cannot really happen.
 */
data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
