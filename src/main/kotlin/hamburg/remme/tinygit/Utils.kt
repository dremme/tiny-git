package hamburg.remme.tinygit

import javafx.animation.Timeline
import javafx.application.Platform
import javafx.beans.binding.IntegerExpression
import javafx.beans.property.IntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.Tooltip
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
private val weeksOrigin = LocalDate.of(1900, 1, 1)

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

fun LocalDate.atEndOfDay() = atTime(LocalTime.MAX)!!

fun LocalDate.atNoon() = atTime(LocalTime.NOON)!!

fun LocalDate.weeksBetween(date: LocalDate) = Math.abs(ChronoUnit.WEEKS.between(weeksOrigin, date)
        - ChronoUnit.WEEKS.between(weeksOrigin, this))

fun printError(message: String) {
    System.err.println(message)
}

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

fun <T : Comparable<T>> ObservableList<T>.addSorted(items: Collection<T>) = items.forEach { item ->
    val index = indexOfFirst { it > item }
    if (index < 0) add(item) else add(index, item)
}

fun <T> ObservableList<T>.addSorted(items: Collection<T>, comparator: (T, T) -> Int) = items.forEach { item ->
    val index = indexOfFirst { comparator.invoke(it, item) > 0 }
    if (index < 0) add(item) else add(index, item)
}

fun <K, V : Comparable<V>> Map<K, V>.takeHighest(count: Int): Map<K, V> {
    return toList().sortedBy { it.second }.takeLast(count).toMap()
}

fun IntegerProperty.inc() = set(get() + 1)

fun IntegerProperty.dec() = set(get() - 1)

fun IntegerExpression.equals0() = isEqualTo(0)!!

fun IntegerExpression.unequals0() = isNotEqualTo(0)!!

fun IntegerExpression.greater0() = greaterThan(0)!!

fun IntegerExpression.greater1() = greaterThan(1)!!

inline fun <T> measureTime(type: String, message: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    val value = block.invoke()
    val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
    val async = if (!Platform.isFxApplicationThread()) "[async]" else ""
    logTypeCharacters = Math.max(logTypeCharacters, type.length)
    val log = String.format("[%6.3fs] %7s %-${logTypeCharacters}s: %s", totalTime, async, type, message)
    if (totalTime < 1) println(log) else printError(log)
    return value
}
