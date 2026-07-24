package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Rebase
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.readFirst
import hamburg.remme.tinygit.readLines
import java.nio.file.Path

private val rebase = arrayOf("rebase")
private val rebaseContinue = arrayOf("rebase", "--continue")
private val rebaseAbort = arrayOf("rebase", "--abort")
private const val APPLY_DIR = "rebase-apply"
private const val MERGE_DIR = "rebase-merge"
private const val NEXT_FILE = "next"
private const val LAST_FILE = "last"
private const val DONE_FILE = "done"
private const val TODO_FILE = "git-rebase-todo"
private const val REBASE_MARKER = "Cannot rebase: "

fun gitIsRebasing(repository: Repository): Boolean {
    val gitDir = repository.path.asPath().resolve(".git")
    return gitDir.resolve(APPLY_DIR).exists() || gitDir.resolve(MERGE_DIR).exists()
}

fun gitRebaseStatus(repository: Repository): Rebase {
    val gitDir = repository.path.asPath().resolve(".git")
    val rebaseApplyDir = gitDir.resolve(APPLY_DIR)
    val rebaseMergeDir = gitDir.resolve(MERGE_DIR)
    return when {
        rebaseApplyDir.exists() -> rebaseApplyDir.parseApply()
        rebaseMergeDir.exists() -> rebaseMergeDir.parseMerge()
        else -> Rebase(0, 0)
    }
}

fun gitRebase(
    repository: Repository,
    branch: Branch,
) {
    val response = git(repository, *rebase, branch.name).trim()
    if (response.startsWith(REBASE_MARKER)) throw RebaseException(response.substringAfter(REBASE_MARKER))
}

fun gitRebaseContinue(repository: Repository) {
    val response = git(repository, *rebaseContinue).trim()
    if (response.contains("needs merge")) throw UnmergedException()
}

fun gitRebaseAbort(repository: Repository) {
    git(repository, *rebaseAbort)
}

private fun Path.parseApply(): Rebase {
    val next = resolve(NEXT_FILE).readFirst().toInt()
    val last = resolve(LAST_FILE).readFirst().toInt()
    return Rebase(next, last)
}

private fun Path.parseMerge(): Rebase {
    val done = resolve(DONE_FILE).readLines().parseLines()
    val todo = resolve(TODO_FILE).readLines().parseLines()
    return Rebase(done, done + todo)
}

private fun Sequence<String>.parseLines() = filterNot { it.isBlank() || it.startsWith("#") }.count()
