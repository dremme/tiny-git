package hamburg.remme.tinygit.domain

class LocalBranch(val shortRef: String, val commitId: String, val remote: Boolean) {

    val local = !remote

    override fun toString() = shortRef

}
