package hamburg.remme.tinygit.domain

import java.time.LocalDateTime

class Commit(val id: String,
             val parents: List<String>,
             val refs: List<String>,
             val fullMessage: String,
             val date: LocalDateTime,
             val authorName: String,
             val authorMail: String) : Comparable<Commit> {

    val shortId: String = id.abbreviate()
    val parentId = if (parents.isEmpty()) "4b825dc642cb6eb9a060e54bf8d69288fbee4904" else parents[0] // special empty tree id
    val shortParents: List<String> = parents.map { it.abbreviate() }
    val shortMessage: String = fullMessage.lines()[0].substringBefore(". ")
    val author = "$authorName <$authorMail>"

    override fun toString() = id

    override fun compareTo(other: Commit) = -date.compareTo(other.date)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Commit

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id.hashCode()

    private fun String.abbreviate() = substring(0, 8)

}
