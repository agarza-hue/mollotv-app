package com.streamvault.app.ui.screens.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.app.ui.screens.mollohome.MolloHomeViewModel
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Schedule deportivo del día. Reusa la categoría sincronizada
 * "🔴 EN VIVO HOY" (category_id=527) y parsea cada canal a un
 * [ParsedSportsEvent] ordenado cronológicamente.
 */
data class SportsScheduleUiState(
    val isLoading: Boolean = true,
    val events: List<ParsedSportsEvent> = emptyList(),
    val totalChannels: Int = 0,
)

@HiltViewModel
class SportsScheduleViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SportsScheduleUiState())
    val state: StateFlow<SportsScheduleUiState> = _state.asStateFlow()

    init {
        load()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun load() {
        viewModelScope.launch {
            providerRepository.getActiveProvider().filterNotNull().collectLatest { provider ->
                _state.value = _state.value.copy(isLoading = true)
                val channels = channelRepository
                    .getChannelsByCategory(provider.id, MolloHomeViewModel.CATEGORY_EN_VIVO_HOY)
                    .first()
                _state.value = SportsScheduleUiState(
                    isLoading = false,
                    events = parseSportsEvents(channels),
                    totalChannels = channels.size,
                )
            }
        }
    }
}
