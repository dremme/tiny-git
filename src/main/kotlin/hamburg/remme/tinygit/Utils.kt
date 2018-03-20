package hamburg.remme.tinygit

import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.IntegerExpression
import javafx.beans.property.IntegerProperty
import javafx.collections.FXCollections
import javafx.scene.control.Tooltip
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
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.streams.toList

val TWO_PI = 360.0
val PI = 180.0
val HALF_PI = 90.0

val daemonFactory = ThreadFactory { Executors.defaultThreadFactory().newThread(it).apply { isDaemon = true } }
val scheduledPool = Executors.newScheduledThreadPool(1, daemonFactory)!!
val cachedPool = Executors.newCachedThreadPool(daemonFactory)!!
val singlePool = Executors.newFixedThreadPool(1, daemonFactory)!!

val homeDir = System.getProperty("user.home")!!

val weekOfMonthFormat = DateTimeFormatter.ofPattern("'Week' W 'of' MMM ''yy")!!
val monthOfYearFormat = DateTimeFormatter.ofPattern("MMM ''yy")!!
val shortDateFormat = DateTimeFormatter.ofPattern("d. MMM yyyy")!!
val dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy")!!
val shortDateTimeFormat = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val dateTimeFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!
var logTypeCharacters = 1
private val temporalOrigin = LocalDate.of(1900, 1, 1)

fun overrideTooltips() {
    val tooltip = Tooltip()
    val fieldBehavior = tooltip.javaClass.getDeclaredField("BEHAVIOR")
    fieldBehavior.isAccessible = true

    val objBehavior = fieldBehavior.get(tooltip)
    val fieldTimer = objBehavior.javaClass.getDeclaredField("activationTimer")
    fieldTimer.isAccessible = true
    (fieldTimer.get(objBehavior) as Timeline).keyFrames.clear()
}

fun systemOffset() = ZoneId.systemDefault().rules.getOffset(Instant.now())!!

fun localDateTime(epochSecond: Long) = LocalDateTime.ofEpochSecond(epochSecond, 0, systemOffset())!!

val LocalDate.startOfDay get() = atStartOfDay()!!

val LocalDate.endOfDay get() = atTime(LocalTime.MAX)!!

val LocalDate.noon get() = atTime(LocalTime.NOON)!!

val LocalDate.daysFromOrigin get() = ChronoUnit.DAYS.between(temporalOrigin, this)

fun LocalDate.daysBetween(date: LocalDate) = Math.abs(this.daysFromOrigin - date.daysFromOrigin)

fun LocalDate.weeksBetween(date: LocalDate) = Math.abs(ChronoUnit.WEEKS.between(temporalOrigin, date)
        - ChronoUnit.WEEKS.between(temporalOrigin, this))

fun String.asResource() = TinyGit::class.java.getResource(this).toExternalForm()!!

fun String.asPath() = Paths.get(this)!!

fun String.asFile() = asPath().toFile()!!

fun Path.exists() = Files.exists(this)

fun Path.delete() = Files.delete(this)

fun Path.read() = Files.readAllBytes(this).toString(StandardCharsets.UTF_8)

fun Path.readLines() = Files.lines(this).use { it.toList() }

fun Path.readFirst() = Files.lines(this).use { it.findFirst() }.orElse("")!!

fun Path.write(text: String) = Files.write(this, text.toByteArray())!!

fun String.normalize() = replace('\\', '/')

fun String.stripParents() = normalize().split('/').last()

fun String.stripHome() = "~${substringAfter(homeDir)}"

fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.htmlEncodeTabs() = replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")

fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

fun String.htmlEncodeAll() = htmlEncode().htmlEncodeTabs().htmlEncodeSpaces()

fun <T> observableList(vararg items: T) = FXCollections.observableArrayList<T>(*items)!!

fun <T> observableList(items: Collection<T>) = FXCollections.observableArrayList<T>(items)!!

inline fun <T, R> List<T>.mapParallel(block: (T) -> R): List<R> {
    return map(block)
}

inline fun <K, V, R> Map<K, V>.mapValuesParallel(block: (V) -> R): Map<K, R> {
    return mapValues { (_, value) -> block(value) }
}

fun <T : Comparable<T>> MutableList<T>.addSorted(items: Collection<T>) = items.forEach { item ->
    val index = indexOfFirst { it > item }
    if (index < 0) add(item) else add(index, item)
}

fun <T> MutableList<T>.addSorted(items: Collection<T>, comparator: (T, T) -> Int) = items.forEach { item ->
    val index = indexOfFirst { comparator(it, item) > 0 }
    if (index < 0) add(item) else add(index, item)
}

inline fun <K, V : Comparable<V>, R : Comparable<R>> Map<K, V>.sortedBy(crossinline block: (Pair<K, V>) -> R?): Map<K, V> {
    return toList().sortedBy(block).toMap()
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getReflective(property: String): T? {
    val field = javaClass.getDeclaredField(property)
    field.isAccessible = true
    return field.get(this) as? T
}

fun IntegerProperty.inc() = set(get() + 1)

fun IntegerProperty.dec() = set(get() - 1)

fun IntegerExpression.equals0() = isEqualTo(0)!!

fun IntegerExpression.unequals0() = isNotEqualTo(0)!!

fun IntegerExpression.greater0() = greaterThan(0)!!

fun IntegerExpression.greater1() = greaterThan(1)!!

val KeyCode.shortName get() = getName().substring(0, Math.min(3, getName().length))

fun printError(message: String) {
    System.err.println(message)
}

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
