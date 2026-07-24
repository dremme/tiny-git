package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val upToDate = mutableSetOf<Repository>() // TODO: move to service?
private val fetch = arrayOf("fetch", "origin")
private val fetchPrune = arrayOf("fetch", "--prune", "origin")
private val pull = arrayOf("pull")

fun gitUpToDate(repository: Repository): Boolean = upToDate.contains(repository)

fun gitFetch(repository: Repository) {
    val response = git(repository, *fetch).trim()
    if (response.lines().any {
            it.startsWith(
                ERROR_SEPARATOR,
            ) ||
                it.startsWith(FATAL_SEPARATOR)
        }
    ) {
        throw FetchException(response.parseError())
    }
    upToDate += repository
}

fun gitFetchPrune(repository: Repository) {
    val response = git(repository, *fetchPrune).trim()
    if (response.lines().any {
            it.startsWith(
                ERROR_SEPARATOR,
            ) ||
                it.startsWith(FATAL_SEPARATOR)
        }
    ) {
        throw FetchException(response.parseError())
    }
    upToDate += repository
}

fun gitPull(repository: Repository) {
    val response = git(repository, *pull).trim()
    if (response.lines().any { it.startsWith(ERROR_SEPARATOR) || it.startsWith(FATAL_SEPARATOR) }) {
        throw PullException(response.parseError())
    } else if (response.lines().any { it.startsWith("CONFLICT") }) {
        throw MergeConflictException()
    }
}

private fun String.parseError(): String = lines().joinToString("\n") { it.substringAfter(ERROR_SEPARATOR).substringAfter(FATAL_SEPARATOR) }
