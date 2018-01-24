package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val upToDate = mutableSetOf<Repository>()
private val fetch = arrayOf("fetch", "origin")
private val fetchPrune = arrayOf("fetch", "--prune", "origin")
private val pull = arrayOf("pull")

fun gitUpToDate(repository: Repository): Boolean {
    return upToDate.contains(repository)
}

fun gitFetch(repository: Repository) {
    git(repository, *fetch).trim()
    upToDate.add(repository)
}

fun gitFetchPrune(repository: Repository) {
    git(repository, *fetchPrune).trim()
    upToDate.add(repository)
}

fun gitPull(repository: Repository) {
    val response = git(repository, *pull).trim()
    if (response.lines().any { it.startsWith(errorSeparator) }) throw PullException(response.parseError())
}

private fun String.parseError(): String {
    return lines()
            .dropWhile { !it.startsWith(errorSeparator) }
            .dropLast(1)
            .joinToString("\n")
            .substringAfter(errorSeparator)
}
