package hamburg.remme.tinygit.domain

import java.time.LocalDateTime

class LocalCommit(val id: String,
                  val shortId: String,
                  val parents: List<String>,
                  val shortParents: List<String>,
                  val fullMessage: String,
                  val shortMessage: String,
                  val date: LocalDateTime,
                  val authorName: String,
                  val authorMail: String) {

    val author = "$authorName <$authorMail>"

    override fun toString() = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalCommit

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id.hashCode()

}
