package hamburg.remme.tinygit.git

class LocalFile(val path: String, val status: Status, val cached: Boolean = true) {

    fun resolve(repository: LocalRepository): String {
        var repositoryPath = repository.path
        if (repositoryPath.contains('\\')) repositoryPath = repositoryPath.replace('\\', '/')
        if (repositoryPath.matches("^[a-zA-Z]:.*".toRegex())) repositoryPath = "/$repositoryPath"
        return "$repositoryPath/$path"
    }

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalFile

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
