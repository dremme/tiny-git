package hamburg.remme.tinygit.domain

class StashEntry(val id: String,
                 val message: String) {

    override fun toString() = message

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StashEntry

        if (id != other.id) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }

}
