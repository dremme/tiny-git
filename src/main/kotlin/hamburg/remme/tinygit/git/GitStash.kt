package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry

private val stash = arrayOf("stash")
private val stashApply = arrayOf("stash", "apply")
private val stashPop = arrayOf("stash", "pop")
private val stashDrop = arrayOf("stash", "drop")
private val stashList = arrayOf("stash", "list")

fun gitStash(repository: Repository) {
    git(repository, *stash) // TODO: is not stashing untracked paths
}

fun gitStashApply(repository: Repository, stashEntry: StashEntry) {
    val response = git(repository, *stashApply, stashEntry.id).trim()
    if (response.lines().any { it.toLowerCase().startsWith("conflict") }) throw StashConflictException()
}

fun gitStashPop(repository: Repository) {
    val response = git(repository, *stashPop).trim()
    if (response.lines().any { it.toLowerCase().startsWith("conflict") }) throw StashConflictException()
}

fun gitStashDrop(repository: Repository, stashEntry: StashEntry) {
    git(repository, *stashDrop, stashEntry.id)
}

fun gitStashList(repository: Repository): List<StashEntry> {
    val entries = mutableListOf<StashEntry>()
    git(repository, *stashList) {
        entries += StashEntry(it.substringBefore(':'), it.substringAfter(':').trim())
    }
    return entries
}
