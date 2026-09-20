package com.example.runway.ui.sheets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.data.remote.api.toUserMessage
import com.example.runway.domain.model.Outfit
import com.example.runway.domain.repository.OutfitRepository
import com.example.runway.domain.repository.PlanRepository
import com.example.runway.ui.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PickOutfitUiState(
    // Set when choosing an outfit for this day.
    val date: LocalDate? = null,
    // Set when choosing a day for this outfit.
    val outfitId: String? = null,
    val outfits: List<Outfit> = emptyList(),
    val days: List<LocalDate> = emptyList(),
    // What's already on each of those days, so the list can say what gets replaced.
    val plannedByDay: Map<LocalDate, Outfit> = emptyMap(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    // The day that was just planned, so the sheet can say so and close.
    val plannedFor: LocalDate? = null,
    val errorMessage: String? = null,
) {
    val choosingOutfit: Boolean get() = date != null
}

class PickOutfitViewModel(
    date: LocalDate?,
    outfitId: String?,
    private val planRepository: PlanRepository,
    private val outfitRepository: OutfitRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickOutfitUiState(date = date, outfitId = outfitId))
    val uiState: StateFlow<PickOutfitUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val outfits = outfitRepository.list().getOrElse {
                _uiState.update { state -> state.copy(isLoading = false, loadFailed = true) }
                return@launch
            }

            if (_uiState.value.choosingOutfit) {
                _uiState.update { it.copy(isLoading = false, outfits = outfits) }
                return@launch
            }

            val start = today()
            val days = (0 until DAYS_AHEAD).map { start.plusDays(it.toLong()) }
            val byId = outfits.associateBy { it.id }
            // Not knowing what's already planned shouldn't stop someone planning.
            val plans = planRepository.between(days.first(), days.last()).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    outfits = outfits,
                    days = days,
                    plannedByDay = plans.mapNotNull { plan -> byId[plan.outfitId]?.let { o -> plan.date to o } }.toMap(),
                )
            }
        }
    }

    fun onPickOutfit(outfitId: String) {
        val date = _uiState.value.date ?: return
        save(date, outfitId)
    }

    fun onPickDay(date: LocalDate) {
        val outfitId = _uiState.value.outfitId ?: return
        save(date, outfitId)
    }

    fun onMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun save(date: LocalDate, outfitId: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            planRepository.plan(date, outfitId)
                .onSuccess { _uiState.update { it.copy(isSaving = false, plannedFor = date) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) } }
        }
    }

    companion object {
        // Planning from an outfit offers the coming week, like the mockup.
        const val DAYS_AHEAD = 7

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                val handle: SavedStateHandle = createSavedStateHandle()
                PickOutfitViewModel(
                    date = handle.get<String>(NavArgs.DATE_ISO)?.let { LocalDate.parse(it) },
                    outfitId = handle.get<String>(NavArgs.OUTFIT_ID),
                    planRepository = app.container.planRepository,
                    outfitRepository = app.container.outfitRepository,
                )
            }
        }
    }
}
