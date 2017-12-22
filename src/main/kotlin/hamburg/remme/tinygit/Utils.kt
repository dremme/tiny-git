package hamburg.remme.tinygit

import javafx.application.Platform
import javafx.collections.FXCollections
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.streams.toList

val shortDateFormat = DateTimeFormatter.ofPattern("d. MMM yyyy")!!
val dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy")!!
val shortDateTimeFormat = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val dateTimeFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!
private val key = SecretKeySpec("FUMN1QLIf8sVkUdv".toByteArray(), "AES")
private val iv = IvParameterSpec("Ay81aeLRJM5xtx9h".toByteArray())

fun printError(message: String) {
    System.err.println(message)
}

fun String.asResource() = TinyGit::class.java.getResource(this).toExternalForm()!!

fun String.asPath() = Paths.get(this)!!

fun Path.exists() = Files.exists(this)

fun Path.delete() = Files.delete(this)

fun Path.read() = Files.readAllBytes(this).toString(StandardCharsets.UTF_8)

fun Path.readLines() = Files.lines(this).use { it.toList() }

fun Path.readFirst() = Files.lines(this).use { it.findFirst() }.orElse("")!!

fun Path.write(text: String) = Files.write(this, text.toByteArray())!!

fun String.normalize() = replace('\\', '/')

fun String.shorten() = normalize().split('/').last()

fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

fun String.htmlEncodeAll() = htmlEncode().htmlEncodeSpaces()

fun String.encrypt() = Cipher.ENCRYPT_MODE.getInstance().doFinal(toByteArray())!!

fun ByteArray.decrypt() = Cipher.DECRYPT_MODE.getInstance().doFinal(this).toString(StandardCharsets.UTF_8)

private fun Int.getInstance(): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(this, key, iv)
    return cipher
}

fun <T> observableList(vararg items: T) = FXCollections.observableArrayList<T>(*items)!!

inline fun <T> measureTime(type: String, message: String, block: () -> T): T {
    if (type.isNotBlank() && message.isNotBlank()) {
        val startTime = System.currentTimeMillis()
        val value = block.invoke()
        val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
        val async = if (!Platform.isFxApplicationThread()) "[async]" else ""
        val log = String.format("[%6.3fs] %7s %-18s: %s", totalTime, async, type, message)
        if (totalTime < 1) println(log) else printError(log)
        return value
    } else {
        return block.invoke()
    }
}
