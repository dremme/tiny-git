package hamburg.remme.tinygit.domain

class GitFile(val path: String, val oldPath: String, val status: Status, val cached: Boolean) : Comparable<GitFile> {

    override fun toString() = path

    override fun compareTo(other: GitFile) = path.compareTo(other.path)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GitFile

        if (path != other.path) return false
        if (status != other.status) return false
        if (cached != other.cached) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + cached.hashCode()
        return result
    }

    enum class Status { CONFLICT, ADDED, COPIED, RENAMED, MODIFIED, REMOVED }

}
