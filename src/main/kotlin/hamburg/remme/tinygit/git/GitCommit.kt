package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val commit = arrayOf("commit")
private val commitAmend = arrayOf("commit", "--amend")

fun gitCommit(repository: Repository, message: String) {
    git(repository, *commit, "-m", message)
}

fun gitCommitAmend(repository: Repository, message: String) {
    git(repository, *commitAmend, "-m", message)
}
