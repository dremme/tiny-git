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

fun gitAddRemote(repository: Repository, url: String) {
    git(repository, *remoteAdd, url)
}

fun gitRemoveRemote(repository: Repository) {
    git(repository, *remoteRemove)
}

fun gitPush(repository: Repository, force: Boolean) {
    var response = git(repository, *if (force) pushForce else push).trim()
    if (response.contains("$fatalSeparator.*no upstream branch".toRegex(setOf(IC, G)))) {
        val name = "${fatalSeparator}The current branch (.+) has no upstream branch\\..*".toRegex(setOf(IC, G)).matchEntire(response)!!.groupValues[1]
        response = git(repository, *if (force) pushForce else push, *upstream, name).trim()
    } else if (response.contains("$fatalSeparator.*does not match.*the name of your current branch".toRegex(setOf(IC, G)))) {
        val head = gitHead(repository)
        response = git(repository, *if (force) pushForce else push, *upstream, head.name).trim()
    }
    if (response.contains("$errorSeparator.*tip of your current branch is behind".toRegex(setOf(IC, G)))) throw BranchBehindException()
    else if (response.contains("$fatalSeparator.*timed out".toRegex(setOf(IC, G)))) throw TimeoutException()
}
