package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry

private val stash = arrayOf("stash")
private val stashPop = arrayOf("stash", "pop")
private val stashList = arrayOf("stash", "list")
private val errorSeparator = "error: "

fun gitStash(repository: Repository) {
    git(repository, *stash)
}

fun gitStashPop(repository: Repository) {
    val response = git(repository, *stashPop)
    println(response)
    if (response.contains(errorSeparator)) throw StashPopException() // TODO
}

fun gitStashList(repository: Repository): List<StashEntry> {
    val entries = mutableListOf<StashEntry>()
    git(repository, *stashList) {}
    return entries
}
