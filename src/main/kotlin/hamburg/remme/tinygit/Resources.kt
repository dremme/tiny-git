package hamburg.remme.tinygit

import java.io.InputStream
import java.net.URL

/**
 * Resolved the given url [String] as [URL].
 */
internal fun String.toURL(): URL = TinyGitApplication::class.java.getResource(this)

/**
 * Resolved the given url [String] as [InputStream].
 */
internal fun String.openStream(): InputStream = toURL().openStream()
