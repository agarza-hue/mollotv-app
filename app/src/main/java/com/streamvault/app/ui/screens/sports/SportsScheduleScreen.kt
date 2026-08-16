package com.streamvault.app.ui.screens.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.streamvault.app.ui.components.shell.AppNavigationChrome
import com.streamvault.app.ui.components.shell.AppScreenScaffold
import com.streamvault.domain.model.Channel

/**
 * Lista cronológica de eventos deportivos del día — derivada de la categoría
 * sincronizada "🔴 EN VIVO HOY". Agrupa por hora (cabecera) y lista cada
 * partido / evento con D-pad navegable. Click reproduce el canal del evento.
 */
@Composable
fun SportsScheduleScreen(
    onPlayChannel: (Channel) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: SportsScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScreenScaffold(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = "Hoy en deportes",
        subtitle = if (state.isLoading) null else "${state.events.size} eventos",
        navigationChrome = AppNavigationChrome.Rail,
        showScreenHeader = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.events.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No hay eventos deportivos sincronizados hoy.\n" +
                                "Verifica que la categoría '🔴 EN VIVO HOY' esté activa en tu provider.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                        )
                    }
                }
                else -> {
                    EventList(
                        events = state.events,
                        onPlay = onPlayChannel,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventList(
    events: List<ParsedSportsEvent>,
    onPlay: (Channel) -> Unit,
) {
    val grouped = remember(events) { events.groupBy { it.hour } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (hour, hourEvents) ->
            item(key = "hour-$hour") {
                HourHeader(hour = hour, count = hourEvents.size)
            }
            items(hourEvents, key = { it.channel.id }) { event ->
                EventRow(event = event, onPlay = { onPlay(event.channel) })
            }
            item(key = "spacer-$hour") { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HourHeader(hour: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%02d:00".format(hour),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$count ${if (count == 1) "evento" else "eventos"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun EventRow(event: ParsedSportsEvent, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .focusable()
            .onFocusChanged { focused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = event.timeHHMM,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.width(72.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
                if (event.isMatch && event.homeTeam != null && event.awayTeam != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${event.homeTeam} · ${event.awayTeam}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (focused) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (focused) "▶  Ver" else "Ver",
                    color = if (focused) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
