package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository

private val reset = arrayOf("reset")
private val resetHard = arrayOf("reset", "--hard")
private val resetSoft = arrayOf("reset", "--soft")

fun gitReset(repository: Repository) {
    git(repository, *reset)
}

fun gitReset(repository: Repository, files: List<File>) {
    git(repository, *reset, *files.map { it.path }.toTypedArray())
}

fun gitResetHard(repository: Repository, branch: Branch) {
    git(repository, *resetHard, "origin/${branch.name}")
}

fun gitSquash(repository: Repository, baseId: String, message: String) {
    git(repository, *resetSoft, baseId)
    gitCommit(repository, message)
}
