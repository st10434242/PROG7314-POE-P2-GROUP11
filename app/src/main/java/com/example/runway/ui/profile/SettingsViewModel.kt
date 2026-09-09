package com.example.runway.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.runway.RunwayApplication
import com.example.runway.domain.model.RunwaySettings
import com.example.runway.domain.model.ThemeOption
import com.example.runway.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.observeSettings()
        .map { SettingsUiState(settings = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun onThemeSelected(theme: ThemeOption) = save { it.copy(theme = theme) }

    fun onBiometricsChanged(enabled: Boolean) = save { it.copy(biometricsEnabled = enabled) }

    fun onNotifyWashChanged(enabled: Boolean) = save { it.copy(notifyWash = enabled) }

    fun onNotifySwapChanged(enabled: Boolean) = save { it.copy(notifySwap = enabled) }

    fun onNotifyWeatherChanged(enabled: Boolean) = save { it.copy(notifyWeather = enabled) }

    /** Ignores a language we do not support rather than saving something we cannot show. */
    fun onLanguageSelected(language: String) {
        if (language !in RunwaySettings.SUPPORTED_LANGUAGES) return
        save { it.copy(language = language) }
    }

    /** The stepper can run past either end, so the value is clamped before it is saved. */
    fun onWearLimitChanged(limit: Int) {
        val safeLimit = limit.coerceIn(RunwaySettings.MIN_WEAR_LIMIT, RunwaySettings.MAX_WEAR_LIMIT)
        save { it.copy(defaultWearLimit = safeLimit) }
    }

    fun onResetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }

    private fun save(change: (RunwaySettings) -> RunwaySettings) {
        viewModelScope.launch {
            repository.update(change(repository.currentSettings()))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RunwayApplication
                SettingsViewModel(app.container.settingsRepository)
            }
        }
    }
}
