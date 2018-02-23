package hamburg.remme.tinygit.domain

open class Branch(val id: String, val name: String, val isRemote: Boolean) : Comparable<Branch> {

    val isLocal = !isRemote

    override fun toString() = name

    override fun compareTo(other: Branch) = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Branch

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

}
