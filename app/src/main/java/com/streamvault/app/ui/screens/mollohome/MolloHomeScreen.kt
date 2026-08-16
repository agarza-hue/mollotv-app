package com.streamvault.app.ui.screens.mollohome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.streamvault.app.ui.components.shell.AppNavigationChrome
import com.streamvault.app.ui.components.shell.AppScreenScaffold
import com.streamvault.app.ui.design.AppColors
import com.streamvault.app.ui.screens.sports.parseSportsEvents
import com.streamvault.domain.model.Channel
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontStyle
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val HERO_PAGES_MAX = 5
private const val HERO_AUTOROTATE_MS = 7_000L

/**
 * Inicio MolloTV — look YT-TV: Hero pager auto-rotativo arriba con los próximos
 * eventos deportivos + carrusel horizontal "EN VIVO HOY" debajo.
 *
 * Theme: dark + accent rojo. Sin paletas brand legacy.
 */
@Composable
fun MolloHomeScreen(
    onEventClick: (categoryId: Long) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onNavigate: (String) -> Unit,
    currentRoute: String,
    viewModel: MolloHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopChrome(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .padding(top = 36.dp),  // espacio para TopChrome
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            Modifier.fillMaxWidth().height(360.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    state.events.isEmpty() -> {
                        EmptyHero()
                    }
                    else -> {
                        HeroPager(
                            events = pickHeroEvents(state.events),
                            onPlay = onPlayChannel
                        )
                    }
                }

                SectionHeader(
                    title = "Hoy en vivo",
                    subtitle = if (state.events.isEmpty()) "Sin eventos sincronizados"
                    else "${state.events.size} eventos · auto-curados desde upstream"
                )

                if (state.events.isNotEmpty()) {
                    EventsCarousel(
                        events = state.events,
                        onClick = { onEventClick(MolloHomeViewModel.CATEGORY_EN_VIVO_HOY) }
                    )
                }
            }
        }
    }
}

/**
 * Pick top N eventos para el Hero — prioriza los próximos a partir de ahora
 * (basado en hora HH:MM extraída del nombre), con fallback a los primeros
 * eventos disponibles si parser falla en todos.
 */
private fun pickHeroEvents(events: List<Channel>): List<Channel> {
    val parsed = parseSportsEvents(events)
    if (parsed.isEmpty()) return events.take(HERO_PAGES_MAX)

    val now = java.util.Calendar.getInstance().let {
        it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE)
    }
    // Eventos cuya hora >= ahora, ordenados por hora ascendente; rellena con
    // primeros del día si no hay suficientes "futuros".
    val upcoming = parsed.filter { it.sortKey >= now }.take(HERO_PAGES_MAX)
    val filler = parsed.take(HERO_PAGES_MAX - upcoming.size)
    return (upcoming + filler.filter { p -> p !in upcoming }).take(HERO_PAGES_MAX).map { it.channel }
}

@Composable
private fun HeroPager(
    events: List<Channel>,
    onPlay: (Channel) -> Unit
) {
    val pageCount = events.size
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val parsed = remember(events) { parseSportsEvents(events) }

    // Auto-rotate cada HERO_AUTOROTATE_MS
    LaunchedEffect(pagerState, pageCount) {
        if (pageCount <= 1) return@LaunchedEffect
        while (true) {
            delay(HERO_AUTOROTATE_MS)
            val next = (pagerState.currentPage + 1) % pageCount
            pagerState.animateScrollToPage(next)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val channel = events[page]
            val event = parsed.getOrNull(page)
            HeroCard(
                channel = channel,
                title = event?.title ?: channel.name,
                timeLabel = event?.timeHHMM,
                isMatch = event?.isMatch == true,
                onClick = { onPlay(channel) }
            )
        }

        // Vertical dots a la derecha del hero (estilo StadioMax)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (i in 0 until pageCount) {
                val isActive = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (isActive) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color.White
                            else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }

        // Counter "X de N" abajo derecha
        if (pageCount > 1) {
            Text(
                text = "${pagerState.currentPage + 1} de $pageCount",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun HeroCard(
    channel: Channel,
    title: String,
    timeLabel: String?,
    isMatch: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0A0A0F),
                        Color(0xFF14141C),
                        Color(0xFF1A0A12)
                    )
                )
            )
            .border(
                BorderStroke(
                    if (focused) 2.dp else 1.dp,
                    if (focused) accent else Color.White.copy(alpha = 0.08f)
                ),
                RoundedCornerShape(20.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        // Vignette derecha → izquierda para legibilidad del texto
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(36.dp)
                .fillMaxWidth(0.75f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiveBadge(accent = accent)
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isMatch) {
                    Text(
                        text = "Partido",
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (focused) accent else Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "▶  Reproducir",
                        color = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    if (pageCount <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        for (i in 0 until pageCount) {
            val isActive = i == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(6.dp)
                    .width(if (isActive) 24.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

@Composable
private fun LiveBadge(accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accent)
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
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    if (focused) 2.dp else 1.dp,
                    if (focused) accent else Color.White.copy(alpha = 0.08f)
                ),
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(if (focused) accent else Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "▶",
                color = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
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
private fun EmptyHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sin eventos hoy", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "El sync corre cada 30 min — vuelve a checar pronto",
                color = Color(0xFFA1A1AA),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Top chrome estilo StadioMax — reloj 24h a la derecha + wordmark MolloTV centrado
 * sobre pill cobalto. Overlay discreto sobre el hero.
 */
@Composable
private fun TopChrome(modifier: Modifier = Modifier) {
    var nowText by remember { mutableStateOf(formatNow24h()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowText = formatNow24h()
            delay(30_000L)  // refresca cada 30s
        }
    }

    Box(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Wordmark centrado, estilo Stadio-italic sobre pill cobalto
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Brand)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "MolloTV",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
        }

        // Reloj a la derecha
        Text(
            text = nowText,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

private fun formatNow24h(): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

