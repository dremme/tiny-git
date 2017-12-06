package hamburg.remme.tinygit.git

class LocalRepository(var path: String = "") {

    val shortPath: String get() = path.split("[\\\\/]".toRegex()).last()
    var ssh: String = ""
    var username: String = ""
    var password: ByteArray = ByteArray(0)
    var proxyHost: String = ""
    var proxyPort: Int = 80

    fun resolve(file: LocalFile): String {
        var normalizedPath = path
        if (normalizedPath.contains('\\')) normalizedPath = normalizedPath.replace('\\', '/')
        if (normalizedPath.matches("^[a-zA-Z]:.*".toRegex())) normalizedPath = "/$normalizedPath"
        return "$normalizedPath/${file.path}"
    }

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
