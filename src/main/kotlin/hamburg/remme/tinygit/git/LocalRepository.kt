package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.normalize
import hamburg.remme.tinygit.shorten

class LocalRepository(var path: String = "") {

    val shortPath: String get() = path.shorten()
    var ssh: String = ""
    var username: String = ""
    var password: ByteArray = ByteArray(0)
    var proxyHost: String = ""
    var proxyPort: Int = 80

    fun resolve(file: LocalFile) = "${path.normalize()}/${file.path}"

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
