package hamburg.remme.tinygit.domain

import java.time.LocalDateTime

open class Commit(val id: String,
                  val parents: List<CommitIsh>,
                  val fullMessage: String,
                  val date: LocalDateTime,
                  val authorName: String,
                  val authorMail: String) : Comparable<Commit> {

    val shortId: String = id.abbreviate()
    val parentId = if (parents.isEmpty()) Head.EMPTY.id else parents[0].id
    val shortParents: List<String> = parents.map { it.id.abbreviate() }
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
