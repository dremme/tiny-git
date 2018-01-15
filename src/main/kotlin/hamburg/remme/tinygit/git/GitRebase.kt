package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Rebase
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.readFirst
import hamburg.remme.tinygit.readLines
import java.nio.file.Path

private val rebase = arrayOf("rebase")
private val rebaseContinue = arrayOf("rebase", "--continue")
private val rebaseAbort = arrayOf("rebase", "--abort")
private val applyDir = "rebase-apply"
private val mergeDir = "rebase-merge"
private val nextFile = "next"
private val lastFile = "last"
private val doneFile = "done"
private val todoFile = "git-rebase-todo"

fun gitIsRebasing(repository: Repository): Boolean {
    val gitDir = repository.path.asPath().resolve(".git")
    return gitDir.resolve(applyDir).exists() || gitDir.resolve(mergeDir).exists()
}

fun gitRebaseStatus(repository: Repository): Rebase {
    val gitDir = repository.path.asPath().resolve(".git")
    val rebaseApplyDir = gitDir.resolve(applyDir)
    val rebaseMergeDir = gitDir.resolve(mergeDir)
    return when {
        rebaseApplyDir.exists() -> rebaseApplyDir.parseApply()
        rebaseMergeDir.exists() -> rebaseMergeDir.parseMerge()
        else -> Rebase(0, 0)
    }
}

fun gitRebase(repository: Repository, branch: String) {
    git(repository, *rebase, branch)
}

fun gitRebaseContinue(repository: Repository) {
    val response = git(repository, *rebaseContinue).trim()
    if (response.contains("needs merge")) throw UnmergedException()
}

fun gitRebaseAbort(repository: Repository) {
    git(repository, *rebaseAbort)
}

private fun Path.parseApply(): Rebase {
    val next = resolve(nextFile).readFirst().toInt()
    val last = resolve(lastFile).readFirst().toInt()
    return Rebase(next, last)
}

private fun Path.parseMerge(): Rebase {
    val done = resolve(doneFile).readLines().parseLines()
    val todo = resolve(todoFile).readLines().parseLines()
    return Rebase(done, done + todo)
}

private fun List<String>.parseLines() = filterNot { it.isBlank() || it.startsWith("#") }.size
