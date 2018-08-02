package hamburg.remme.tinygit

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Returns the SLF4J logger for the reified class.
 */
internal inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)
