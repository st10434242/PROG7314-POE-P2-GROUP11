package com.example.runway.ui.items

import com.example.runway.domain.model.Item

// Screen state for the wardrobe list (IIE, 2026).

data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    // True while a sync is in flight.
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
) {
    // True when at least one item is waiting to reach the server.
    val hasUnsyncedChanges: Boolean get() = items.any { it.pendingSync }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
