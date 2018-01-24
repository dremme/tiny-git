package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val gc = arrayOf("gc", "--aggressive")

fun gitGc(repository: Repository) {
    git(repository, *gc)
}
