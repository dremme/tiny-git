package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry

private val stash = arrayOf("stash")
private val stashPop = arrayOf("stash", "pop")
//private val stashList = arrayOf("stash", "list")

fun gitStash(repository: Repository) {
    git(repository, *stash) // TODO: is not stashing untracked paths
}

fun gitStashPop(repository: Repository) {
    val response = git(repository, *stashPop)
    if (response.contains(errorSeparator)) throw StashPopException() // TODO
}

fun gitStashList(repository: Repository): List<StashEntry> {
    val entries = mutableListOf<StashEntry>()
    // TODO: very slow operation
//    git(repository, *stashList) {
//        entries += StashEntry(it.substringBefore(':'), it.substringAfter(':'))
//    }
    return entries
}
