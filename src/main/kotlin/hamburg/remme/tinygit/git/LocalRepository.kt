package hamburg.remme.tinygit.git

class LocalRepository(var path: String = "") {

    var ssh: String = ""
    var username: String = ""
    var password: String = ""
    var proxyHost: String? = null // TODO: should not be nullable
    var proxyPort: Int? = 80 // TODO: should not be nullable

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
