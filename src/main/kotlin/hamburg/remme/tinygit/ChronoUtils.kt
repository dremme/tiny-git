package hamburg.remme.tinygit

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Converts a [String] into an [Instant] as if it is in epoch second form.
 * @see Instant.ofEpochSecond
 */
internal fun String.toInstant(): Instant = Instant.ofEpochSecond(toLong())

/**
 * Converts an [Instant] to a [LocalDateTime], using the given time zone.
 * @param timeZone the time zone of the time.
 * @see LocalDateTime.ofInstant
 */
internal fun Instant.toDateTime(timeZone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(this, timeZone)

/**
 * Measures the time between the [Instant] and now, using the given time zone.
 * @param unit     the unit to measure the time in.
 * @param timeZone the time zone of the time.
 */
internal fun Instant.untilNow(unit: ChronoUnit, timeZone: ZoneId): Long = until(Instant.now(), unit, timeZone)

/**
 * Measures the time between the [Instant] and the given time, using the given time zone.
 * @param time     the time until to measure.
 * @param unit     the unit to measure the time in.
 * @param timeZone the time zone of the time.
 */
internal fun Instant.until(time: Instant, unit: ChronoUnit, timeZone: ZoneId): Long {
    return unit.between(toDateTime(timeZone), time.toDateTime(timeZone))
}
