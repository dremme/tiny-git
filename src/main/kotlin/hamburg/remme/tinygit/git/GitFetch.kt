package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val upToDate = mutableSetOf<Repository>() // TODO: move to service?
private val fetch = arrayOf("fetch", "origin")
private val fetchPrune = arrayOf("fetch", "--prune", "origin")
private val pull = arrayOf("pull")

fun gitUpToDate(repository: Repository): Boolean {
    return upToDate.contains(repository)
}

fun gitFetch(repository: Repository) {
    val response = git(repository, *fetch).trim()
    if (response.lines().any { it.startsWith(errorSeparator) || it.startsWith(fatalSeparator) }) throw FetchException(response.parseError())
    upToDate += repository
}

fun gitFetchPrune(repository: Repository) {
    val response = git(repository, *fetchPrune).trim()
    if (response.lines().any { it.startsWith(errorSeparator) || it.startsWith(fatalSeparator) }) throw FetchException(response.parseError())
    upToDate += repository
}

fun gitPull(repository: Repository) {
    val response = git(repository, *pull).trim()
    if (response.lines().any { it.startsWith(errorSeparator) || it.startsWith(fatalSeparator) }) throw PullException(response.parseError())
    else if (response.lines().any { it.startsWith("CONFLICT") }) throw MergeConflictException()
}

private fun String.parseError(): String {
    return lines().joinToString("\n") { it.substringAfter(errorSeparator).substringAfter(fatalSeparator) }
}
