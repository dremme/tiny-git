package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.normalize
import hamburg.remme.tinygit.stripParents

class Repository(var path: String = "") {

    val shortPath get() = path.stripParents()

    fun resolve(file: File) = "${path.normalize()}/${file.path}"

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Repository

        if (path != other.path) return false

        return true
    }

    override fun hashCode() = path.hashCode()

}
