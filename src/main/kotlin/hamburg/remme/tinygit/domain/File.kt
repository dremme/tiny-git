package hamburg.remme.tinygit.domain

class File(val path: String, val oldPath: String, val status: Status, val isCached: Boolean) : Comparable<File> {

    override fun toString() = path

    override fun compareTo(other: File) = path.compareTo(other.path)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (path != other.path) return false
        if (status != other.status) return false
        if (isCached != other.isCached) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + isCached.hashCode()
        return result
    }

    enum class Status { CONFLICT, ADDED, COPIED, RENAMED, MODIFIED, REMOVED }

}
