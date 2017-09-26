package hamburg.remme.tinygit.git

import java.time.LocalDateTime

class LocalCommit(val id: String,
                  val shortId: String,
                  val parents: List<String>,
                  val fullMessage: String,
                  val shortMessage: String,
                  val date: LocalDateTime,
                  val author: String,
                  val branches: List<LocalBranch>) {

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalCommit

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
