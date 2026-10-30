package dev.nocturne.ambient.data

import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class CalendarRepository {
    private val mutex = Mutex()
    private var lastFetchAt = 0L
    private var cached: List<CalendarEvent> = emptyList()
    private var lastSuccessfulFetch = 0L

    suspend fun get(settings: Settings, force: Boolean = false): List<CalendarEvent> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val urls = settings.calendarUrls
                .lineSequence()
                .flatMap { it.split(',', ' ', '\n') }
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .toList()
            if (urls.isEmpty()) {
                cached = emptyList()
                return@withContext emptyList()
            }
            val now = System.currentTimeMillis()
            if (!force && now - lastFetchAt < REFRESH_MS) return@withContext cached
            lastFetchAt = now
            val results = mutableListOf<CalendarEvent>()
            var successes = 0
            urls.forEachIndexed { index, rawUrl ->
                val url = if (rawUrl.startsWith("webcal://")) {
                    "https://" + rawUrl.removePrefix("webcal://")
                } else rawUrl
                if (!url.startsWith("https://")) return@forEachIndexed
                try {
                    val body = Http.getText(
                        url,
                        timeoutMs = 15_000,
                        maxBytes = 3 * 1024 * 1024,
                        accept = "text/calendar, text/plain, */*"
                    )
                    val name = feedName(body) ?: "Calendar ${index + 1}"
                    results += IcsParser.parse(body, name)
                    successes++
                } catch (_: Exception) {
                    // Keep other feeds and the previous cache.
                }
            }
            if (successes > 0) {
                cached = results
                lastSuccessfulFetch = now
            }
            cached
                .asSequence()
                .filter { it.end.isAfter(ZonedDateTime.now().minusMinutes(30)) }
                .filter { it.start.isBefore(ZonedDateTime.now().plusDays(32)) }
                .distinctBy { "${it.title}|${it.start.toInstant()}" }
                .sortedBy { it.start.toInstant() }
                .take(12)
                .toList()
        }
    }

    fun peek(): List<CalendarEvent> = cached

    private fun feedName(body: String): String? = body.lineSequence()
        .firstOrNull { it.startsWith("X-WR-CALNAME:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.take(64)
        ?.takeIf { it.isNotBlank() }

    companion object {
        private const val REFRESH_MS = 30 * 60 * 1000L
    }
}

internal object IcsParser {
    private data class Property(
        val value: String,
        val params: Map<String, String>
    )

    private data class RawEvent(
        val title: String,
        val start: ZonedDateTime,
        val end: ZonedDateTime,
        val allDay: Boolean,
        val location: String?,
        val recurrence: Recurrence?
    )

    private data class Recurrence(
        val freq: String,
        val interval: Int,
        val count: Int?,
        val until: ZonedDateTime?,
        val byDay: Set<DayOfWeek>
    )

    fun parse(body: String, calendarName: String): List<CalendarEvent> {
        val lines = unfold(body)
        val events = mutableListOf<CalendarEvent>()
        var inside = false
        var properties = mutableMapOf<String, Property>()

        for (line in lines) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inside = true
                    properties = mutableMapOf()
                }
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    if (inside) {
                        parseEvent(properties, calendarName)?.let(events::addAll)
                    }
                    inside = false
                }
                inside -> {
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        val left = line.substring(0, colon)
                        val value = line.substring(colon + 1)
                        val name = left.substringBefore(';').uppercase(Locale.US)
                        val params = left.substringAfter(';', "")
                            .split(';')
                            .mapNotNull { part ->
                                val eq = part.indexOf('=')
                                if (eq <= 0) null
                                else part.substring(0, eq).uppercase(Locale.US) to
                                    part.substring(eq + 1).trim('"')
                            }
                            .toMap()
                        properties[name] = Property(unescape(value), params)
                    }
                }
            }
        }
        return events
    }

    private fun parseEvent(properties: Map<String, Property>, calendarName: String): List<CalendarEvent>? {
        val title = properties["SUMMARY"]?.value?.takeIf { it.isNotBlank() } ?: "Untitled event"
        val startProp = properties["DTSTART"] ?: return null
        val (start, allDay) = parseMoment(startProp) ?: return null
        val endProp = properties["DTEND"]
        val end = endProp?.let { parseMoment(it)?.first } ?: if (allDay) {
            start.plusDays(1)
        } else {
            start.plusHours(1)
        }
        val recurrence = properties["RRULE"]?.value?.let(::parseRecurrence)
        val raw = RawEvent(
            title = title,
            start = start,
            end = end,
            allDay = allDay,
            location = properties["LOCATION"]?.value?.takeIf { it.isNotBlank() },
            recurrence = recurrence
        )
        val now = ZonedDateTime.now()
        val windowStart = now.minusDays(1)
        val windowEnd = now.plusDays(31)
        return occurrences(raw, windowStart, windowEnd).map { occurrenceStart ->
            CalendarEvent(
                title = raw.title,
                start = occurrenceStart,
                end = occurrenceStart.plus(Duration.between(raw.start, raw.end)),
                allDay = raw.allDay,
                location = raw.location,
                calendarName = calendarName
            )
        }
    }

    private fun occurrences(
        raw: RawEvent,
        windowStart: ZonedDateTime,
        windowEnd: ZonedDateTime
    ): List<ZonedDateTime> {
        val recurrence = raw.recurrence ?: return if (
            raw.end.isAfter(windowStart) && raw.start.isBefore(windowEnd)
        ) listOf(raw.start) else emptyList()

        val result = mutableListOf<ZonedDateTime>()
        val duration = Duration.between(raw.start, raw.end)
        var cursor = raw.start
        var produced = 0
        var iterations = 0

        if (recurrence.count == null) {
            val days = ChronoUnit.DAYS.between(raw.start.toLocalDate(), windowStart.toLocalDate())
            if (days > 0) {
                cursor = when (recurrence.freq) {
                    "DAILY" -> raw.start.plusDays((days / recurrence.interval) * recurrence.interval)
                    "WEEKLY" -> raw.start.plusWeeks(
                        (days / 7 / recurrence.interval) * recurrence.interval
                    )
                    "MONTHLY" -> raw.start.plusMonths(
                        (days / 30 / recurrence.interval) * recurrence.interval
                    )
                    "YEARLY" -> raw.start.plusYears(
                        (days / 365 / recurrence.interval) * recurrence.interval
                    )
                    else -> cursor
                }
                if (cursor.isAfter(windowStart)) {
                    cursor = when (recurrence.freq) {
                        "DAILY" -> cursor.minusDays(recurrence.interval.toLong())
                        "WEEKLY" -> cursor.minusWeeks(recurrence.interval.toLong())
                        "MONTHLY" -> cursor.minusMonths(recurrence.interval.toLong())
                        "YEARLY" -> cursor.minusYears(recurrence.interval.toLong())
                        else -> cursor
                    }
                }
            }
        }

        var weekCursor = cursor.toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        while (cursor.isBefore(windowEnd) && iterations++ < 3000) {
            if (recurrence.count != null && produced >= recurrence.count) break
            if (recurrence.until != null && cursor.isAfter(recurrence.until)) break

            val dayMatches = recurrence.byDay.isEmpty() || cursor.dayOfWeek in recurrence.byDay
            if (dayMatches && cursor.plus(duration).isAfter(windowStart)) {
                result += cursor
            }
            produced++

            cursor = when (recurrence.freq) {
                "DAILY" -> cursor.plusDays(recurrence.interval.toLong())
                "WEEKLY" -> {
                    if (recurrence.byDay.isEmpty()) {
                        cursor.plusWeeks(recurrence.interval.toLong())
                    } else {
                        val time = cursor.toLocalTime()
                        val zone = cursor.zone
                        var next: ZonedDateTime? = null
                        for (week in 0..64) {
                            val monday = weekCursor.plusWeeks(week.toLong())
                            for (day in recurrence.byDay.sortedBy { it.value }) {
                                val candidateDate = monday.plusDays((day.value - 1).toLong())
                                val candidate = candidateDate.atTime(time).atZone(zone)
                                if (candidate.isAfter(cursor)) {
                                    next = candidate
                                    break
                                }
                            }
                            if (next != null) break
                        }
                        next ?: cursor.plusWeeks(recurrence.interval.toLong())
                    }
                }
                "MONTHLY" -> {
                    val next = cursor.plusMonths(recurrence.interval.toLong())
                    if (next.dayOfMonth < cursor.dayOfMonth && next.month != cursor.month) {
                        next.withDayOfMonth(1)
                    } else next
                }
                "YEARLY" -> cursor.plusYears(recurrence.interval.toLong())
                else -> break
            }
        }
        return result
    }

    private fun parseRecurrence(value: String): Recurrence? {
        val parts = value.split(';').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq < 0) null else part.substring(0, eq).uppercase(Locale.US) to part.substring(eq + 1)
        }.toMap()
        val freq = parts["FREQ"]?.uppercase(Locale.US) ?: return null
        if (freq !in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) return null
        val byDay = parts["BYDAY"]?.split(',')?.mapNotNull { token ->
            when (token.trim().takeLast(2).uppercase(Locale.US)) {
                "MO" -> DayOfWeek.MONDAY
                "TU" -> DayOfWeek.TUESDAY
                "WE" -> DayOfWeek.WEDNESDAY
                "TH" -> DayOfWeek.THURSDAY
                "FR" -> DayOfWeek.FRIDAY
                "SA" -> DayOfWeek.SATURDAY
                "SU" -> DayOfWeek.SUNDAY
                else -> null
            }
        }?.toSet() ?: emptySet()
        val until = parts["UNTIL"]?.let { rawUntil ->
            parseMoment(Property(rawUntil, emptyMap()))?.first
        }
        return Recurrence(
            freq = freq,
            interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            count = parts["COUNT"]?.toIntOrNull()?.coerceAtLeast(1),
            until = until,
            byDay = byDay
        )
    }

    private fun parseMoment(property: Property): Pair<ZonedDateTime, Boolean>? {
        val value = property.value.trim()
        if (value.isBlank()) return null
        val dateOnly = value.length == 8 && value.all(Char::isDigit)
        if (dateOnly) {
            val date = runCatching { LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) }
                .getOrNull() ?: return null
            return date.atStartOfDay(ZoneId.systemDefault()) to true
        }
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm[ss]")
        return if (value.endsWith("Z", ignoreCase = true)) {
            val local = runCatching {
                LocalDateTime.parse(value.dropLast(1), formatter)
            }.getOrNull() ?: return null
            local.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault()) to false
        } else if (value.length > 15 && (value[value.length - 5] == '+' || value[value.length - 5] == '-')) {
            val offset = value.takeLast(5)
            val local = runCatching {
                LocalDateTime.parse(value.dropLast(5), formatter)
            }.getOrNull() ?: return null
            val zone = runCatching { java.time.ZoneOffset.of(offset) }.getOrNull() ?: return null
            OffsetDateTime.of(local, zone).atZoneSameInstant(ZoneId.systemDefault()) to false
        } else {
            val local = runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
                ?: return null
            val zone = property.params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                ?: ZoneId.systemDefault()
            local.atZone(zone) to false
        }
    }

    private fun unfold(body: String): List<String> {
        val normalized = body.replace("\r\n", "\n").replace('\r', '\n')
        val result = mutableListOf<String>()
        for (line in normalized.split('\n')) {
            if ((line.startsWith(' ') || line.startsWith('\t')) && result.isNotEmpty()) {
                result[result.lastIndex] = result.last() + line.substring(1)
            } else {
                result += line
            }
        }
        return result
    }

    private fun unescape(value: String): String = value
        .replace("\\n", "\n", ignoreCase = true)
        .replace("\\N", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
