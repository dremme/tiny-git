package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists

private val merge = arrayOf("merge")
private val mergeContinue = arrayOf("merge", "--continue")
private val mergeAbort = arrayOf("merge", "--abort")

fun gitIsMerging(repository: Repository): Boolean {
    return repository.path.asPath().resolve(".git").resolve("MERGE_HEAD").exists()
}

fun gitMerge(repository: Repository, branch: String) {
    git(repository, *merge, branch)
}

fun gitMergeContinue(repository: Repository) {
    git(repository, *mergeContinue)
}

fun gitMergeAbort(repository: Repository) {
    git(repository, *mergeAbort)
}
