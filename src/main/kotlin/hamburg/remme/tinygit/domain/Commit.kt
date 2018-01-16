package hamburg.remme.tinygit.domain

import java.time.LocalDateTime

class Commit(val id: String,
             val shortId: String,
             val parents: List<String>,
             val shortParents: List<String>,
             val refs: List<String>,
             val fullMessage: String,
             val shortMessage: String,
             val date: LocalDateTime,
             val authorName: String,
             val authorMail: String) {

    val author = "$authorName <$authorMail>"
    val parentId = if (parents.isEmpty()) "4b825dc642cb6eb9a060e54bf8d69288fbee4904" else parents[0] // special empty tree id

    override fun toString() = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Commit

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id.hashCode()

}
