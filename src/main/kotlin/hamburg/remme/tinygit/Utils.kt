package hamburg.remme.tinygit

import javafx.application.Platform
import javafx.beans.binding.IntegerExpression
import javafx.beans.property.IntegerProperty
import javafx.collections.FXCollections
import javafx.scene.input.KeyCode
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadFactory
import kotlin.streams.toList

/**
 * Constant for 360°. Degrees of 2*pi.
 */
const val TWO_PI = 360.0
/**
 * Constant for 180°. Degrees of pi.
 */
const val PI = 180.0
/**
 * Constant for 90°. Degrees of pi/2.
 */
const val HALF_PI = 90.0
/**
 * **Warning: do not use this!**
 */
var logTypeCharacters = 1
private val daemonFactory = ThreadFactory { Executors.defaultThreadFactory().newThread(it).apply { isDaemon = true } }
/**
 * Single-threaded scheduler with standard daemon thread factory.
 */
val scheduledPool = Executors.newScheduledThreadPool(1, daemonFactory)!!
/**
 * Openly cached thread pool with standard daemon thread factory.
 */
val cachedPool = Executors.newCachedThreadPool(daemonFactory)!!
/**
 * Single-threaded thread pool with standard daemon thread factory.
 */
val singlePool = Executors.newFixedThreadPool(1, daemonFactory)!!
/**
 * A thread pool for fork-join operations.
 */
val forkJoinPool = ForkJoinPool.commonPool()

val dayOfWeekFormat = DateTimeFormatter.ofPattern("EEE")!!
val monthOfYearFormat = DateTimeFormatter.ofPattern("MMM ''yy")!!
val shortDateFormat = DateTimeFormatter.ofPattern("d. MMM yyyy")!!
val dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy")!!
val shortDateTimeFormat = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val dateTimeFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!
/**
 * The timezone offset of the local machine.
 */
val systemOffset get() = ZoneId.systemDefault().rules.getOffset(Instant.now())!!
/**
 * The number of days since 1900-01-01.
 */
val LocalDate.daysFromOrigin get() = ChronoUnit.DAYS.between(temporalOrigin, this)
private val temporalOrigin = LocalDate.of(1900, 1, 1)

/**
 * Returns a [LocalDate] which is equal to the Monday of its week.
 */
fun LocalDate.atStartOfWeek() = minusDays(dayOfWeek.value - 1L)!!

/**
 * Returns a [LocalDateTime] which is equal to the end of the day, its last nanosecond.
 */
fun LocalDate.atEndOfDay() = atTime(LocalTime.MAX)!!

/**
 * Returns a [LocalDateTime] which is equal to the middle of the day.
 */
fun LocalDate.atNoon() = atTime(LocalTime.NOON)!!

/**
 * A UTC based [LocalDateTime] from a given [epochSecond].
 */
fun localDateTime(epochSecond: Long) = LocalDateTime.ofEpochSecond(epochSecond, 0, systemOffset)!!

/**
 * Absolute number of days between two [LocalDate]s.
 */
fun LocalDate.daysBetween(date: LocalDate) = Math.abs(this.daysFromOrigin - date.daysFromOrigin)

/**
 * Absolute number of weeks between two [LocalDate]s.
 */
fun LocalDate.weeksBetween(date: LocalDate) = Math.abs(ChronoUnit.WEEKS.between(temporalOrigin, date)
        - ChronoUnit.WEEKS.between(temporalOrigin, this))

/**
 * A local resource as externalized URL, e.g.
 *
 * ```kotlin
 * stage.scene.stylesheets += "default.css".asResource()
 * ```
 *
 * gets the externalized form of that CSS file and is added the scene's stylesheets.
 */
fun String.asResource() = TinyGit::class.java.getResource(this).toExternalForm()!!

/**
 * Creates a [Path] from the string.
 */
fun String.asPath() = Paths.get(this)!!

/**
 * Creates a [java.io.File] from the string.
 */
fun String.asFile() = asPath().toFile()!!

/**
 * @return `true` if the [Path] exists on the machine.
 */
fun Path.exists() = Files.exists(this)

/**
 * Deletes the [Path]. May throw an error if deletion is not permitted.
 *
 * @throws java.io.IOException if the [Path] does not exist, is a directory or deletion is not permitted.
 */
fun Path.delete() = Files.delete(this)

/**
 * Reads all bytes of the [Path] as UTF-8 string.
 */
fun Path.read() = Files.readAllBytes(this).toString(StandardCharsets.UTF_8)

/**
 * Reads all lines and returns a [List] of strings.
 * May return an empty list if the [Path] is empty.
 */
fun Path.readLines() = Files.lines(this).use { it.toList() }

/**
 * Reads only the first line of the [Path].
 * May return an empty string if the [Path] is empty.
 */
fun Path.readFirst() = Files.lines(this).use { it.findFirst() }.orElse("")!!

/**
 * Writes the given [text] to the [Path]. Any existing bytes are overwritten.
 */
fun Path.write(text: String) = Files.write(this, text.toByteArray())!!

/**
 * Normalizes `\` to `/`, meaning it may normalize Windows-style paths to UNIX-style.
 */
fun String.normalize() = replace('\\', '/')

/**
 * Strips all path-like parents from the string.
 */
fun String.stripParents() = normalize().split('/').last()

/**
 * Replaces the home path with `~` to get a UNIX-style home folder representation.
 */
fun String.stripHome() = "~${substringAfter(homeDir)}"

/**
 * HTML encodes `&`, `<` and `>` to their respective HTML codes.
 */
fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

/**
 * HTML encodes tabs to four HTML non-breaking spaces.
 */
fun String.htmlEncodeTabs() = replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")

/**
 * Encodes spaces to HTML non-breaking spaces.
 */
fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

/**
 * Calls [htmlEncode], [htmlEncodeTabs], [htmlEncodeSpaces] in that order.
 */
fun String.htmlEncodeAll() = htmlEncode().htmlEncodeTabs().htmlEncodeSpaces()

/**
 * Creates a new [javafx.collections.ObservableList] from the given [items].
 */
fun <T> observableList(vararg items: T) = FXCollections.observableArrayList<T>(*items)!!

/**
 * Creates a new [javafx.collections.ObservableList] from the given [items].
 */
fun <T> observableList(items: Collection<T>) = FXCollections.observableArrayList<T>(items)!!

/**
 * [List.map] but parallel using a [ForkJoinPool].
 */
inline fun <T, R> List<T>.mapParallel(crossinline block: (T) -> R): List<R> {
    return map { forkJoinPool.submit(Callable { block(it) }) }
            .onEach { it.fork() }
            .map { it.join() }
}

/**
 * [Map.mapValues] but parallel using a [ForkJoinPool].
 */
inline fun <K, V, R> Map<K, V>.mapValuesParallel(crossinline block: (V) -> R): Map<K, R> {
    return mapValues { (_, it) -> forkJoinPool.submit(Callable { block(it) }) }
            .onEach { (_, it) -> it.fork() }
            .mapValues { (_, it) -> it.join() }
}

/**
 * Adds the given [items] sorted to the list. The [items] have implement [Comparable].
 */
fun <T : Comparable<T>> MutableList<T>.addSorted(items: Collection<T>) = items.forEach { item ->
    val index = indexOfFirst { it > item }
    if (index < 0) add(item) else add(index, item)
}

/**
 * Adds the given [items] sorted to the list using the given [comparator] function.
 */
fun <T> MutableList<T>.addSorted(items: Collection<T>, comparator: (T, T) -> Int) = items.forEach { item ->
    val index = indexOfFirst { comparator(it, item) > 0 }
    if (index < 0) add(item) else add(index, item)
}

/**
 * Creates a sorted [Map] similar to [List.sortedBy]. The original [Map] is unchanged.
 */
inline fun <K, V : Comparable<V>, R : Comparable<R>> Map<K, V>.sortedBy(crossinline block: (Pair<K, V>) -> R?): Map<K, V> {
    return toList().sortedBy(block).toMap()
}

/**
 * Equal to `i.set(i.get() + 1)`.
 */
fun IntegerProperty.inc() = set(get() + 1)

/**
 * Equal to `i.set(i.get() - 1)`.
 */
fun IntegerProperty.dec() = set(get() - 1)

/**
 * Equal to `i.isEqualTo(0)`.
 */
fun IntegerExpression.equals0() = isEqualTo(0)!!

/**
 * Equal to `i.isNotEqualTo(0)`.
 */
fun IntegerExpression.unequals0() = isNotEqualTo(0)!!

/**
 * Equal to `i.greaterThan(0)`.
 */
fun IntegerExpression.greater0() = greaterThan(0)!!

/**
 * Equal to `i.greaterThan(1)`.
 */
fun IntegerExpression.greater1() = greaterThan(1)!!

/**
 * The three letter name of the [KeyCode].
 */
val KeyCode.shortName get() = getName().substring(0, Math.min(3, getName().length))

/**
 * Prints an error to [System.err].
 */
fun printError(message: String) {
    System.err.println(message)
}

/**
 * Measures an logs the needed time to execute the given [block].
 */
inline fun <T> measureTime(type: String, message: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    val value = block()
    val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
    val async = if (!Platform.isFxApplicationThread()) "[async]" else ""
    logTypeCharacters = Math.max(logTypeCharacters, type.length)
    val log = String.format("[%6.3fs] %7s %-${logTypeCharacters}s: %s", totalTime, async, type, message)
    if (totalTime < 1) println(log) else printError(log)
    return value
}
