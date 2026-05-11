package com.streamvault.app.ui.screens.mollohome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.app.ui.components.shell.AppNavigationChrome
import com.streamvault.app.ui.components.shell.AppScreenScaffold
import com.streamvault.app.ui.design.AppColors
import com.streamvault.domain.model.Channel

private val Pink = Color(0xFFB43072)
private val Navy = Color(0xFF092D76)
private val Purple = Color(0xFF8F00C3)
private val Crimson = Color(0xFFBE123D)

/**
 * Inicio MolloTV — pantalla destacada con look OpticTV: hero pink→navy con
 * stripes diagonales + carrusel horizontal de "🔴 EN VIVO HOY".
 *
 * Click en un evento → navega al LIVE_TV con categoryId 527 (el resto lo
 * maneja el HomeScreen existente).
 */
@Composable
fun MolloHomeScreen(
    onEventClick: (categoryId: Long) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: MolloHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    AppScreenScaffold(
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        title = "Inicio MolloTV",
        subtitle = null,
        navigationChrome = AppNavigationChrome.Rail,
        showScreenHeader = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black, Color(0xFF050507))
                    )
                )
        ) {
            // Stripes decorativas (OpticTV signature)
            DiagonalStripe(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-80).dp, y = 60.dp)
                    .height(380.dp)
                    .alpha(0.55f)
            )
            DiagonalStripe(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 80.dp, y = 240.dp)
                    .height(220.dp)
                    .alpha(0.30f)
            )
            DiagonalStripe(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 220.dp, y = (-80).dp)
                    .height(280.dp)
                    .alpha(0.22f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                HeroCard(
                    featured = state.featured,
                    totalEvents = state.events.size,
                    onClick = { onEventClick(MolloHomeViewModel.CATEGORY_EN_VIVO_HOY) }
                )

                SectionHeader(
                    title = "🔴 EN VIVO HOY",
                    subtitle = "${state.events.size} eventos · auto-curados desde upstream"
                )

                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Pink)
                        }
                    }
                    state.events.isEmpty() -> {
                        EmptyState()
                    }
                    else -> {
                        EventsCarousel(
                            events = state.events,
                            onClick = { onEventClick(MolloHomeViewModel.CATEGORY_EN_VIVO_HOY) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagonalStripe(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(6.dp)
            .rotate(90f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(listOf(Pink, Color(0xFF030187)))
            )
    )
}

@Composable
private fun HeroCard(featured: Channel?, totalEvents: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF050507),
                        Color(0xFF1A0512),
                        Color(0xFF4A0A2A),
                        Pink,
                        Navy
                    )
                )
            )
            .onFocusChanged { /* could glow on focus later */ }
            .focusable()
    ) {
        // Inner diagonal stripe accent
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-80).dp, y = (-20).dp)
                .height(280.dp)
                .width(5.dp)
                .rotate(90f)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(Pink, Color(0xFF030187))))
                .alpha(0.55f)
        )

        // Left fade overlay for text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LiveBadge()
                Text(
                    text = "$totalEvents eventos hoy · actualizado cada 30 min",
                    color = Color(0xFFD4D4D8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = featured?.name ?: "Sincronizando eventos del día…",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp
            )
            if (featured != null) {
                Text(
                    text = "Desliza al carrusel para ver toda la programación",
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Crimson)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "EN VIVO",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = Color(0xFFA1A1AA),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EventsCarousel(events: List<Channel>, onClick: () -> Unit) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 32.dp)
    ) {
        items(events, key = { it.id }) { ch ->
            EventTile(ch, onClick)
        }
    }
}

@Composable
private fun EventTile(channel: Channel, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .then(
                if (focused) Modifier.border(
                    BorderStroke(2.dp, Brush.verticalGradient(listOf(Pink, Navy))),
                    RoundedCornerShape(12.dp)
                )
                else Modifier.border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    RoundedCornerShape(12.dp)
                )
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        // Soft event-style background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.Black,
                            Pink.copy(alpha = 0.35f),
                            Navy.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        // Bottom dark overlay for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        // Play pill top-left
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Pink, Navy))),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        // Event name overlay bottom
        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sin eventos hoy", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "El sync corre cada 30 min — vuelve a checar pronto",
                color = Color(0xFFA1A1AA),
                fontSize = 13.sp
            )
        }
    }
}

