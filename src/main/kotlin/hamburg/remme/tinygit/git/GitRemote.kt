package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val remoteGetUrl = arrayOf("remote", "get-url", "origin")
private val fetch = arrayOf("fetch", "origin")
private val fetchPrune = arrayOf("fetch", "--prune", "origin")
private val push = arrayOf("push")
private val pushForce = arrayOf("push", "--force")
private val upstream = arrayOf("--set-upstream", "origin")
private val pull = arrayOf("pull")
private val errorSeparator = "error: "
private val fatalSeparator = "fatal: "
private val IC = RegexOption.IGNORE_CASE
private val G = RegexOption.DOT_MATCHES_ALL

fun gitGetRemoteUrl(repository: Repository): String {
    return git(repository, *remoteGetUrl)
}

fun gitHasRemote(repository: Repository): Boolean {
    return !gitGetRemoteUrl(repository).startsWith(fatalSeparator)
}

fun gitFetch(repository: Repository) {
    gitProxy(repository)
    val response = git(repository, *fetch).trim()
    println(response) // TODO
}

fun gitFetchPrune(repository: Repository) {
    gitProxy(repository)
    val response = git(repository, *fetchPrune).trim()
    println(response) // TODO
}

fun gitPush(repository: Repository, force: Boolean) {
    gitProxy(repository)
    val response = git(repository, *if (force) pushForce else push).trim()
    println(response) // TODO
    if (response.contains("$fatalSeparator.*no upstream branch".toRegex(IC))) gitPush(repository, response.trimBranch(), force)
    else if (response.contains("$errorSeparator.*tip of your current branch is behind".toRegex(IC))) throw PushException()
    else if (response.contains("$fatalSeparator.*timed out".toRegex(IC))) throw TimeoutException()
}

fun gitPush(repository: Repository, branch: String, force: Boolean) {
    gitProxy(repository)
    val response = git(repository, *if (force) pushForce else push, *upstream, branch).trim()
    println(response) // TODO
    if (response.contains("$errorSeparator.*tip of your current branch is behind".toRegex(IC))) throw PushException()
    else if (response.contains("$fatalSeparator.*timed out".toRegex(IC))) throw TimeoutException()
}

fun gitPull(repository: Repository) {
    val response = git(repository, *pull).trim()
    println(response) // TODO
    if (response.lines().any { it.startsWith(errorSeparator) }) throw PullException(response.trimError())
}

private fun String.trimError(): String {
    return lines()
            .dropWhile { !it.startsWith(errorSeparator) }
            .dropLast(1)
            .joinToString("\n")
            .substringAfter(errorSeparator)
}

private fun String.trimBranch(): String {
    return "${fatalSeparator}The current branch (.+) has no upstream branch\\..*"
            .toRegex(setOf(IC, G))
            .matchEntire(this)!!.groupValues[1]
}
