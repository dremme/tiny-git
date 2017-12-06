package hamburg.remme.tinygit.git

class LocalFile(val path: String, val status: Status, val cached: Boolean = true) {

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalFile

        if (path != other.path) return false
        if (cached != other.cached) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + cached.hashCode()
        return result
    }

    enum class Status { CONFLICT, ADDED, COPIED, RENAMED, MODIFIED, REMOVED }

}
