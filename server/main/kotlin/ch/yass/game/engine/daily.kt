package ch.yass.game.engine

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

val zurich: ZoneId = ZoneId.of("Europe/Zurich")

/**
 * Since this is the SWISS national game, daily challenges are aligned with the time in Switzerland. If you're two hours
 * behind Swiss time, the daily challenge will start already at 22:00 in your country.
 */
fun swissDay(utcNow: LocalDateTime): LocalDate = utcNow.toInstant(ZoneOffset.UTC).atZone(zurich).toLocalDate()

data class UtcWindow(val start: LocalDateTime, val end: LocalDateTime)

/**
 * The UTC timestamps a Swiss day starts and ends at, e.g. 2026-07-27 in Switzerland runs from 2026-07-26T22:00 until
 * 2026-07-27T22:00 UTC. [UtcWindow.end] is exclusive, so compare with `>= start` and `< end`.
 */
fun swissDayWindowUTC(day: LocalDate): UtcWindow =
    UtcWindow(
        start = day.atStartOfDay(zurich).toLocalDateTimeUTC(),
        end = day.plusDays(1).atStartOfDay(zurich).toLocalDateTimeUTC()
    )

private fun ZonedDateTime.toLocalDateTimeUTC(): LocalDateTime =
    withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
