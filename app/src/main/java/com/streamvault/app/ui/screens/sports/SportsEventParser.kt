package com.streamvault.app.ui.screens.sports

import com.streamvault.domain.model.Channel

/**
 * Parsea nombres de canales de la categoría "🔴 EN VIVO HOY" sincronizada
 * desde upstream IPTV. Patrón observado:
 *
 *   "02:45 Moto GP Francia"              -> evento individual
 *   "11:00 Celta vs Levante"             -> match
 *   "13:00 | Cavaliers vs Pistons"       -> match con prefijo `| `
 *   "13:07 | Blue-Jays vs Angels"        -> idem
 *
 * Si el nombre no encaja el patrón (no empieza con HH:MM), se devuelve null.
 */
data class ParsedSportsEvent(
    val channel: Channel,
    val timeHHMM: String,         // "13:00"
    val hour: Int,                // 13
    val minute: Int,              // 0
    val title: String,            // "Cavaliers vs Pistons" o "Moto GP Francia"
    val homeTeam: String?,        // "Cavaliers" o null si evento individual
    val awayTeam: String?,        // "Pistons" o null si evento individual
    val isMatch: Boolean          // true si tiene "vs", false si evento individual
) {
    val sortKey: Int get() = hour * 60 + minute
}

private val EVENT_PATTERN = Regex(
    """^\s*(\d{1,2}):(\d{2})\s*\|?\s*(.+?)\s*$""",
    RegexOption.IGNORE_CASE
)

private val VS_SPLIT = Regex("""\s+vs\s+""", RegexOption.IGNORE_CASE)

fun parseSportsEvent(channel: Channel): ParsedSportsEvent? {
    val match = EVENT_PATTERN.matchEntire(channel.name) ?: return null
    val (hh, mm, rest) = match.destructured
    val hour = hh.toIntOrNull() ?: return null
    val minute = mm.toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null

    val parts = rest.split(VS_SPLIT, limit = 2)
    val isMatch = parts.size == 2
    val home = if (isMatch) parts[0].trim() else null
    val away = if (isMatch) parts[1].trim() else null
    val title = if (isMatch) "${home} vs ${away}" else rest.trim()

    return ParsedSportsEvent(
        channel = channel,
        timeHHMM = "%02d:%02d".format(hour, minute),
        hour = hour,
        minute = minute,
        title = title,
        homeTeam = home,
        awayTeam = away,
        isMatch = isMatch,
    )
}

fun parseSportsEvents(channels: List<Channel>): List<ParsedSportsEvent> =
    channels.mapNotNull(::parseSportsEvent).sortedBy { it.sortKey }
