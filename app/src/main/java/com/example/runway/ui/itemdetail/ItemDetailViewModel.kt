package com.example.runway.ui.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Reads its navigation argument from SavedStateHandle rather than being
 * handed it by the composable - so the id survives process death.
 */
class ItemDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ItemRepository
) : ViewModel() {

    private val itemId: Long = checkNotNull(savedStateHandle[Routes.ARG_ITEM_ID]) {
        "ItemDetailViewModel requires a ${Routes.ARG_ITEM_ID} argument"
    }

    val uiState: StateFlow<ItemDetailUiState> = repository.observeItem(itemId)
        .map { ItemDetailUiState(item = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemDetailUiState()
        )

    fun onDelete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(itemId)
            onDeleted()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ItemDetailViewModel(createSavedStateHandle(), app.container.itemRepository)
            }
        }
    }
}
