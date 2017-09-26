package hamburg.remme.tinygit.git

class LocalFile(val path: String, val status: Status) {

    override fun toString(): String {
        return path
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalFile

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    enum class Status { ADDED, CHANGED, MODIFIED, REMOVED, UNTRACKED }

}
