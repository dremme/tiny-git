package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository

private val add = arrayOf("add")
private val rm = arrayOf("rm")

fun gitAdd(repository: Repository) {
    git(repository, *add, ".")
}

fun gitAdd(repository: Repository, files: List<File>) {
    git(repository, *add, *files.map { it.path }.toTypedArray())
}

fun gitAddUpdate(repository: Repository) {
    git(repository, *add, "--update", ".")
}

fun gitRemove(repository: Repository, files: List<File>) {
    git(repository, *rm, *files.map { it.path }.toTypedArray())
}
