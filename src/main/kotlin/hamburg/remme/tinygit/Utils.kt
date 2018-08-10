package hamburg.remme.tinygit

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * The relative file representing the current directory, in other words `.`.
 */
internal val CURRENT_DIR: File = File(".")

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

/**
 * Copy of the default implementation to load UTF-8 encoded resource bundles.
 */
internal class UTF8Support : ResourceBundle.Control() {

    override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle {
        val resourceName = toResourceName(toBundleName(baseName, locale), "properties")
        val stream = if (reload) reload(loader, resourceName) else loader.getResourceAsStream(resourceName)
        return PropertyResourceBundle(InputStreamReader(stream, StandardCharsets.UTF_8))
    }

    private fun reload(loader: ClassLoader, resourceName: String): InputStream {
        val conn = loader.getResource(resourceName).openConnection()
        conn.useCaches = false
        return conn.getInputStream()
    }

}
