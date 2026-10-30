package dev.nocturne.ambient.ui

import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class TimeParts(
    val hour: String,
    val minute: String,
    val second: String,
    val suffix: String
)

fun timeParts(time: LocalTime, use24Hour: Boolean): TimeParts {
    val h = if (use24Hour) time.hour else ((time.hour + 11) % 12) + 1
    return TimeParts(
        hour = if (use24Hour) h.toString().padStart(2, '0') else h.toString(),
        minute = time.minute.toString().padStart(2, '0'),
        second = time.second.toString().padStart(2, '0'),
        suffix = if (use24Hour) "" else if (time.hour < 12) "AM" else "PM"
    )
}

fun formatClock(time: LocalTime, use24Hour: Boolean, seconds: Boolean = false): String {
    val p = timeParts(time, use24Hour)
    val base = "${p.hour}:${p.minute}" + if (seconds) ":${p.second}" else ""
    return if (p.suffix.isEmpty()) base else "$base ${p.suffix}"
}

fun formattedDate(date: java.time.LocalDate, includeWeekday: Boolean = true): String {
    val pattern = if (includeWeekday) "EEEE, d MMMM" else "d MMMM"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

fun shortDate(date: java.time.LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

fun temperature(valueC: Double, celsius: Boolean): String {
    val value = if (celsius) valueC else valueC * 9 / 5 + 32
    return "${value.roundToInt()}°"
}

fun wind(valueKph: Double, celsius: Boolean): String {
    val value = if (celsius) valueKph else valueKph * 0.621371
    val unit = if (celsius) "km/h" else "mph"
    return "${value.roundToInt()} $unit"
}

fun dayLabel(date: java.time.LocalDate, today: java.time.LocalDate): String = when (date) {
    today -> "TODAY"
    today.plusDays(1) -> "TOMORROW"
    else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault())
}

fun eventTime(event: CalendarEvent, use24Hour: Boolean): String {
    if (event.allDay) return "All day"
    val local = event.start.withZoneSameInstant(ZoneId.systemDefault())
    return formatClock(local.toLocalTime(), use24Hour)
}

fun nextEvent(events: List<CalendarEvent>, now: ZonedDateTime = ZonedDateTime.now()): CalendarEvent? {
    return events.firstOrNull { it.end.isAfter(now) } ?: events.firstOrNull()
}

fun sunProgress(now: LocalDateTime, sunrise: LocalTime?, sunset: LocalTime?): Float {
    if (sunrise == null || sunset == null) return 0.5f
    val rise = now.toLocalDate().atTime(sunrise)
    val set = now.toLocalDate().atTime(sunset)
    if (set <= rise) return 0.5f
    val fraction = Duration.between(rise, now).toMinutes().toFloat() /
        Duration.between(rise, set).toMinutes().coerceAtLeast(1).toFloat()
    return fraction.coerceIn(0f, 1f)
}

fun isDaytime(now: LocalDateTime, sunrise: LocalTime?, sunset: LocalTime?): Boolean {
    if (sunrise == null || sunset == null) return now.hour in 6..18
    val time = now.toLocalTime()
    return time >= sunrise && time < sunset
}

fun moonPhaseFraction(date: java.time.LocalDate = java.time.LocalDate.now()): Float {
    // Reference new moon: 2000-01-06 18:14 UTC.
    val reference = java.time.Instant.parse("2000-01-06T18:14:00Z")
    val now = date.atStartOfDay(ZoneId.of("UTC")).toInstant()
    val days = Duration.between(reference, now).toMinutes() / 1440.0
    val synodic = 29.530588853
    var phase = (days % synodic) / synodic
    if (phase < 0) phase += 1.0
    return phase.toFloat()
}

fun moonIllumination(phase: Float): Int {
    val illum = (1 - cos(2 * PI * phase)) / 2
    return (illum * 100).roundToInt()
}

fun moonName(phase: Float): String = when {
    phase < 0.03f || phase > 0.97f -> "New moon"
    phase < 0.22f -> "Waxing crescent"
    phase < 0.28f -> "First quarter"
    phase < 0.47f -> "Waxing gibbous"
    phase < 0.53f -> "Full moon"
    phase < 0.72f -> "Waning gibbous"
    phase < 0.78f -> "Last quarter"
    else -> "Waning crescent"
}

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

fun worldClock(timezone: String, use24Hour: Boolean, now: ZonedDateTime = ZonedDateTime.now()): String {
    val zoned = now.withZoneSameInstant(ZoneId.of(timezone))
    val h = if (use24Hour) zoned.hour else ((zoned.hour + 11) % 12) + 1
    val hh = if (use24Hour) h.toString().padStart(2, '0') else h.toString()
    val mm = zoned.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

fun worldDayDelta(timezone: String, now: ZonedDateTime = ZonedDateTime.now()): String {
    val zone = ZoneId.of(timezone)
    val local = now.withZoneSameInstant(zone).toLocalDate()
    val today = now.toLocalDate()
    return when {
        local.isAfter(today) -> "+1"
        local.isBefore(today) -> "−1"
        else -> ""
    }
}

fun smoothShift(minutes: Long, amplitude: Float, enabled: Boolean): Float {
    if (!enabled) return 0f
    val period = 23.0
    return (sin(minutes.toDouble() / period) * amplitude).toFloat()
}
