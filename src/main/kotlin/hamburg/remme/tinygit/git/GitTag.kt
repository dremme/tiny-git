package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.Tag

private val tag = arrayOf("tag")
private val tagDelete = arrayOf("tag", "--delete")
private val tagList = arrayOf("tag")
private val tagParse = arrayOf("rev-parse")
private val tagPush = arrayOf("push", "origin")
private val tagDeletePush = arrayOf("push", "--delete", "origin")

fun gitTag(repository: Repository, commit: Commit, name: String) {
    val response = git(repository, *tag, name, commit.id)
    if (response.contains("$fatalSeparator.*already exists".toRegex(setOf(IC, G)))) throw TagAlreadyExistsException()
    git(repository, *tagPush, name)
}

fun gitTagDelete(repository: Repository, tag: Tag) {
    git(repository, *tagDelete, tag.name)
    git(repository, *tagDeletePush, tag.name)
}

fun gitTagList(repository: Repository): List<Tag> {
    val tags = mutableListOf<Tag>()
    git(repository, *tagList) {
        val id = git(repository, *tagParse, it).trim()
        tags += Tag(it, id)
    }
    return tags
}
