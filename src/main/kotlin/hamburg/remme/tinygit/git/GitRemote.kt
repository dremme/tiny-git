package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val remoteGetUrl = arrayOf("remote", "get-url", "origin")
private val remoteSetUrl = arrayOf("remote", "set-url", "origin")
private val remoteGetPushUrl = arrayOf("remote", "get-url", "--push", "origin")
private val remoteSetPushUrl = arrayOf("remote", "set-url", "--push", "origin")
private val remoteAdd = arrayOf("remote", "add", "origin")
private val remoteRemove = arrayOf("remote", "remove", "origin")
private val push = arrayOf("push")
private val pushForce = arrayOf("push", "--force")
private val upstream = arrayOf("--set-upstream", "origin")

fun gitGetUrl(repository: Repository): String {
    val response = git(repository, *remoteGetUrl).trim()
    if (response.startsWith(fatalSeparator)) return ""
    return response
}

fun gitSetUrl(repository: Repository, url: String) {
    git(repository, *remoteSetUrl, url)
}

fun gitGetPushUrl(repository: Repository): String {
    val response = git(repository, *remoteGetPushUrl).trim()
    if (response.startsWith(fatalSeparator)) return ""
    return response
}

fun gitSetPushUrl(repository: Repository, url: String) {
    git(repository, *remoteSetPushUrl, url)
}

fun gitHasRemote(repository: Repository): Boolean {
    return gitGetUrl(repository).isNotBlank()
}

fun gitAddRemote(repository: Repository, url: String) {
    git(repository, *remoteAdd, url)
}

fun gitRemoveRemote(repository: Repository) {
    git(repository, *remoteRemove)
}

fun gitPush(repository: Repository, force: Boolean) {
    gitSetProxy(repository) // TODO: should only be configured once
    val response = git(repository, *if (force) pushForce else push).trim()
    if (response.contains("$fatalSeparator.*no upstream branch".toRegex(IC))) gitPush(repository, response.parseBranchName(), force)
    else if (response.contains("$errorSeparator.*tip of your current branch is behind".toRegex(IC))) throw PushException()
    else if (response.contains("$fatalSeparator.*timed out".toRegex(IC))) throw TimeoutException()
}

fun gitPush(repository: Repository, branch: String, force: Boolean) {
    gitSetProxy(repository) // TODO: should only be configured once
    val response = git(repository, *if (force) pushForce else push, *upstream, branch).trim()
    if (response.contains("$errorSeparator.*tip of your current branch is behind".toRegex(IC))) throw PushException()
    else if (response.contains("$fatalSeparator.*timed out".toRegex(IC))) throw TimeoutException()
}

private fun String.parseBranchName(): String {
    return "${fatalSeparator}The current branch (.+) has no upstream branch\\..*"
            .toRegex(setOf(IC, G))
            .matchEntire(this)!!.groupValues[1]
}
