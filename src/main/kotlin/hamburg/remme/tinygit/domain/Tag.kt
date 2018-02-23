package hamburg.remme.tinygit.domain

class Tag(val name: String) : Comparable<Tag> {

    override fun compareTo(other: Tag) = name.compareTo(other.name)

}
