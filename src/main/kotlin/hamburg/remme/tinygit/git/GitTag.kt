package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val tagList = arrayOf("tag")

fun gitTagList(repository: Repository): List<String> {
    val tags = mutableListOf<String>()
    git(repository, *tagList) { tags += it }
    return tags
}
