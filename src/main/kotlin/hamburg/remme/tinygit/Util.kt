package hamburg.remme.tinygit

import java.io.BufferedReader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.util.stream.Stream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

val SHORT_DATE = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val FULL_DATE = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!
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

fun Path.write(text: String) = Files.write(this, text.toByteArray())!!

inline fun <T> String.lines(block: (Stream<String>) -> T) = BufferedReader(StringReader(this)).lines()!!

fun String.normalize() = replace('\\', '/')

fun String.shorten() = normalize().split('/').last()

fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

fun String.htmlEncodeAll() = htmlEncode().htmlEncodeSpaces()

fun String.encrypt(): ByteArray {
    val cipher = cipher(Cipher.ENCRYPT_MODE)
    return cipher.doFinal(toByteArray())
}

fun ByteArray.decrypt(): String {
    val cipher = cipher(Cipher.DECRYPT_MODE)
    return cipher.doFinal(this).toString(StandardCharsets.UTF_8)
}

private fun cipher(mode: Int): Cipher {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(mode, key, iv)
    return cipher
}
