package hamburg.remme.tinygit

import java.time.Instant

/**
 * Converts a [String] into an [Instant] as if it is in epoch second form.
 *
 * @see Instant.ofEpochSecond
 */
internal fun String.toInstant(): Instant = Instant.ofEpochSecond(toLong())

/**
 * Splits a [String] by the given [delimiter]. Will be [emptyList] if the [String] is blank.
 *
 * @param delimiter defaults to `" "`.
 */
internal fun String.safeSplit(delimiter: String = " "): List<String> = takeIf(String::isNotBlank)?.split(delimiter) ?: emptyList()
