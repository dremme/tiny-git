package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists

private val merge = arrayOf("merge")
private val mergeContinue = arrayOf("merge", "--continue")
private val mergeAbort = arrayOf("merge", "--abort")

fun gitIsMerging(repository: Repository): Boolean {
    return repository.path.asPath().resolve(".git").resolve("MERGE_HEAD").exists()
}

fun gitMerge(repository: Repository, branch: Branch) {
    val response = git(repository, *merge, branch.name).trim()
    if (response.lines().any { it.startsWith(errorSeparator) }) throw MergeException()
    else if (response.lines().any { it.startsWith("CONFLICT") }) throw MergeConflictException()
}

fun gitMergeContinue(repository: Repository) {
    git(repository, *mergeContinue)
}

fun gitMergeAbort(repository: Repository) {
    git(repository, *mergeAbort)
}
