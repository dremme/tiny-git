package hamburg.remme.tinygit

import java.io.InputStream
import java.net.URL

/**
 * Resolved the given url [String] as [URL].
 *
 * @param url must be a classpath URL.
 */
fun resource(url: String): URL = TinyGitApplication::class.java.getResource(url)

/**
 * Resolved the given url [String] as [InputStream].
 *
 * @param url must be a classpath URL.
 */
fun resourceStream(url: String): InputStream = resource(url).openStream()

/**
 * Resolved the given url [String] as stringified [URL].
 *
 * @param url must be a classpath URL.
 */
fun resourceString(url: String): String = resource(url).toExternalForm()
