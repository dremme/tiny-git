package hamburg.remme.tinygit.git

class LocalRepository(var path: String = "") {

    var credentials: LocalCredentials? = null
    var proxyHost: String? = null
    var proxyPort: Int? = 80

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalRepository

        if (path != other.path) return false

        return true
    }

    override fun hashCode() = path.hashCode()

}
