package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.Tag

private val tagList = arrayOf("tag")

fun gitTagList(repository: Repository): List<Tag> {
    val tags = mutableListOf<Tag>()
    git(repository, *tagList) { tags += Tag(it) }
    return tags
}
