package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val commit = arrayOf("commit")
private val commitAmend = arrayOf("commit", "--amend")

fun gitCommit(
    repository: Repository,
    message: String,
) {
    val response = git(repository, *commit, "-m", message)
    if (response.lines().any { it.startsWith(ERROR_SEPARATOR) || it.startsWith(FATAL_SEPARATOR) }) throw RuntimeException()
}

fun gitCommitAmend(
    repository: Repository,
    message: String,
) {
    val response = git(repository, *commitAmend, "-m", message)
    if (response.lines().any { it.startsWith(ERROR_SEPARATOR) || it.startsWith(FATAL_SEPARATOR) }) throw RuntimeException()
}
