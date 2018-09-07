package hamburg.remme.tinygit

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.net.URL

/**
 * Resolves the given url [String] as [URL].
 */
internal fun String.toURL(): URL = App::class.java.getResource(this)

/**
 * Resolves the given url [String] in the externalized URL form.
 */
internal fun String.toExternal(): String = toURL().toExternalForm()

/**
 * Resolves the given url [String] as [InputStream].
 */
internal fun String.openStream(): InputStream = toURL().openStream()

/**
 * Resolved the given url [String] as plain text.
 */
internal fun String.loadText(): String = openStream().bufferedReader().readText()

/**
 * Resolved the given url [String] as YAML object, most likely a [Map].
 */
internal fun <T> String.loadYaml(): T = Yaml().load(loadText())
