package hamburg.remme.tinygit.domain

class Branch(val name: String, val commitId: String, val isRemote: Boolean) {

    val isLocal = !isRemote

    override fun toString() = name

}
