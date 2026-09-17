package com.example.runway.ui.outfits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.local.OutfitRenderStore
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.Item
import com.example.runway.domain.repository.ItemRepository
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.RatingRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel for editing one saved outfit (IIE, 2026).

class OutfitDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val outfitRepository: OutfitRepository,
    private val itemRepository: ItemRepository,
    private val renderStore: OutfitRenderStore,
    private val ratingRepository: RatingRepository,
) : ViewModel() {

    private val outfitId: String = checkNotNull(savedStateHandle[NavArgs.OUTFIT_ID]) {
        "OutfitDetailViewModel requires a ${NavArgs.OUTFIT_ID} argument"
    }

    private val _uiState = MutableStateFlow(OutfitDetailUiState())
    val uiState: StateFlow<OutfitDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        loadRatings()
    }

    private fun load() {
        viewModelScope.launch {
            outfitRepository.get(outfitId)
                .onSuccess { outfit ->
                    // The outfit stores ids; the names come from the wardrobe, and
                    // an id with no matching garment is simply dropped.
                    val wardrobe = itemRepository.observeItems().first()
                    val garments = outfit.itemIds.mapNotNull { id -> wardrobe.firstOrNull { it.id == id } }

                    _uiState.update {
                        it.copy(
                            outfit = outfit,
                            garments = garments,
                            name = outfit.name,
                            render = renderStore.load(outfit.renderPath),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    // Ratings load separately from the outfit so a rating failure cannot blank the screen.
    private fun loadRatings() {
        viewModelScope.launch {
            ratingRepository.listForOutfit(outfitId)
                .onSuccess { summary -> _uiState.update { it.copy(ratings = summary) } }
        }
    }

    // Called when the sheet closes, so a new rating shows without leaving the screen.
    fun onRatingsChanged() = loadRatings()

    fun onWoreToday() {
        viewModelScope.launch {
            outfitRepository.logWear(outfitId)
                .onSuccess {
                    _uiState.update { it.copy(wearLoggedAt = System.currentTimeMillis()) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.toUserMessage()) }
                }
        }
    }

    fun onWearHandled() = _uiState.update { it.copy(wearLoggedAt = null) }

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value) }

    fun onRemoveGarment(item: Item) = _uiState.update { state ->
        state.copy(garments = state.garments.filterNot { it.id == item.id })
    }

    fun onSave() {
        val state = _uiState.value
        val outfit = state.outfit ?: return
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Give the outfit a name") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            outfitRepository.update(
                outfit.copy(name = name, itemIds = state.garments.map { it.id })
            )
                .onSuccess { saved ->
                    _uiState.update {
                        it.copy(outfit = saved, isSaving = false, savedAt = System.currentTimeMillis())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) }
                }
        }
    }

    fun onDelete() {
        val renderPath = _uiState.value.outfit?.renderPath
        viewModelScope.launch {
            outfitRepository.delete(outfitId)
                .onSuccess {
                    // The render is this device's copy, so it goes with the outfit.
                    renderStore.delete(renderPath)
                    _uiState.update { it.copy(deleted = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.toUserMessage()) } }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null, savedAt = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                OutfitDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    outfitRepository = app.container.outfitRepository,
                    itemRepository = app.container.itemRepository,
                    renderStore = app.container.outfitRenderStore,
                    ratingRepository = app.container.ratingRepository,
                )
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
*/
