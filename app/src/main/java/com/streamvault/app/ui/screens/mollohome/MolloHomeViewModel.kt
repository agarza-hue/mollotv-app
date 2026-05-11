package com.streamvault.app.ui.screens.mollohome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.Channel
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
 * Pantalla "Inicio MolloTV" — muestra la categoría sincronizada "🔴 EN VIVO HOY"
 * con hero + carrusel horizontal estilo OpticTV.
 *
 * Hardcoded category_id = 527 porque es la user_category creada por
 * iptv-events-sync.py (juntas-app/scripts/). Si no hay sync corrido, la lista
 * llega vacía y el screen muestra estado "sin eventos".
 */
@HiltViewModel
class MolloHomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MolloHomeUiState())
    val state: StateFlow<MolloHomeUiState> = _state.asStateFlow()

    init {
        loadEvents()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEvents() {
        viewModelScope.launch {
            providerRepository.getActiveProvider().filterNotNull().collectLatest { provider ->
                _state.value = _state.value.copy(isLoading = true)
                val events = channelRepository
                    .getChannelsByCategory(provider.id, CATEGORY_EN_VIVO_HOY)
                    .first()
                _state.value = MolloHomeUiState(
                    isLoading = false,
                    events = events,
                    featured = events.firstOrNull()
                )
            }
        }
    }

    companion object {
        // Mantén sincronizado con scripts/iptv-events-sync.py USER_CATEGORY_NAME
        const val CATEGORY_EN_VIVO_HOY = 527L
    }
}

data class MolloHomeUiState(
    val isLoading: Boolean = true,
    val events: List<Channel> = emptyList(),
    val featured: Channel? = null
)
