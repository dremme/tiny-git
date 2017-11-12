package hamburg.remme.tinygit.git

class LocalStashEntry(val id: String,
                      val message: String) {

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
