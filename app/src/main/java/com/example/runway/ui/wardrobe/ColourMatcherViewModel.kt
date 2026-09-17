package com.example.runway.ui.wardrobe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.runway.RunwayApplication
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ViewModel for the smart colour matcher (IIE, 2026).
// The garment the user came from is the reference every other item is measured against.

class ColourMatcherViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ItemRepository
) : ViewModel() {
    private val itemId: String = checkNotNull(savedStateHandle[NavArgs.ITEM_ID]) {
        "ColourMatcherViewModel requires a ${NavArgs.ITEM_ID} argument"
    }

    val uiState: StateFlow<ColourMatcherUiState> = repository.observeItem(itemId)
        .map { ColourMatcherUiState(reference = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ColourMatcherUiState()
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ColourMatcherViewModel(createSavedStateHandle(), app.container.itemRepository)
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/