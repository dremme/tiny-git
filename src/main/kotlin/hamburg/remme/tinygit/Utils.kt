package hamburg.remme.tinygit

import javafx.collections.ObservableList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

private val OS = System.getProperty("os.name")
/**
 * The running OS is Windows.
 */
internal val IS_WINDOWS: Boolean = OS.startsWith("Windows")
/**
 * The running OS is Mac OS.
 */
internal val IS_MAC: Boolean = OS.startsWith("Mac")
/**
 * The running OS is some kind of Linux or UNIX. This could also be embedded or Android.
 */
internal val IS_LINUX: Boolean = OS.startsWith("Linux")
/**
 * The relative file representing the current directory, in other words `.`.
 */
internal val CURRENT_DIR: File = File(".")
/**
 * The name of a Git repository subfolder.
 */
internal const val GIT_DIR: String = ".git"

/**
 * Returns the SLF4J logger for the reified class.
 */
internal inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)

/**
 * Splits a [String] by the given [delimiter]. Will be [emptyList] if the [String] is blank.
 * @param delimiter defaults to `" "`.
 */
internal fun String.safeSplit(delimiter: String = " "): List<String> = takeIf(String::isNotBlank)?.split(delimiter).orEmpty()

/**
 * A file is a Git repository if it is a directory and contains a `.git` subfolder. This might be a relatively slow
 * function to call.
 */
internal fun File.isGitRepository(): Boolean {
    return isDirectory && list().any { it.endsWith(GIT_DIR, true) }
}

/**
 * Joins all lines of the reader to a [String] separated by `\n`.
 */
internal fun BufferedReader.join(): String = readLines().joinToString("\n")

/**
 * Convenience method to use [ObservableList.setAll] with a [Sequence].
 */
internal fun <T> ObservableList<T>.setAll(sequence: Sequence<T>): Boolean = setAll(sequence.toList())

/**
 * Opens the default system viewer/editor for the given URI. This can also be used to browse to a certain URL.
 * @param uri the URI to open.
 */
internal fun openURI(uri: String) {
    val command = when {
        IS_WINDOWS -> "rundll32 url.dll,FileProtocolHandler $uri"
        IS_MAC -> "open $uri"
        IS_LINUX -> "python -m webbrowser $uri" // FIXME: risky but might always work
        else -> throw UnsupportedOperationException("$OS is not supported.")
    }
    Runtime.getRuntime().exec(command)
}

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
