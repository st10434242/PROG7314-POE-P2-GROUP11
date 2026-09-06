package com.example.runway.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.domain.model.Item
import com.example.runway.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    // Errors raised by user actions (as opposed to by the data stream).
    private val actionError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ItemsUiState> =
        combine(
            repository.observeItems()
                .map<List<Item>, Result<List<Item>>> { Result.success(it) }
                .catch { emit(Result.failure(it)) },
            actionError.asStateFlow()
        ) { result, error ->
            result.fold(
                onSuccess = { items ->
                    ItemsUiState(items = items, isLoading = false, errorMessage = error)
                },
                onFailure = { throwable ->
                    ItemsUiState(isLoading = false, errorMessage = throwable.message ?: "Could not load items.")
                }
            )
        }.stateIn(
            scope = viewModelScope,
            // Keep collecting for 5s after the screen goes away so a rotation
            // does not re-query the database.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemsUiState()
        )

    fun onAddItem(title: String, note: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            actionError.value = "A title is required."
            return
        }
        viewModelScope.launch {
            runCatching { repository.save(Item(title = cleanTitle, note = note.trim())) }
                .onFailure { actionError.value = it.message ?: "Could not save the item." }
                .onSuccess { actionError.value = null }
        }
    }

    fun onDeleteItem(id: Long) {
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { actionError.value = it.message ?: "Could not delete the item." }
        }
    }

    fun onErrorShown() {
        actionError.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                ItemsViewModel(app.container.itemRepository)
            }
        }
    }
}
