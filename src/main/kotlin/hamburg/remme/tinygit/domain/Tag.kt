package hamburg.remme.tinygit.domain

class Tag(val name: String, val id: String) : Comparable<Tag> {

    override fun toString() = name

    override fun compareTo(other: Tag) = name.compareTo(other.name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tag

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
