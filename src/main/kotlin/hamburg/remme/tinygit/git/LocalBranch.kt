package hamburg.remme.tinygit.git

class LocalBranch(val shortRef: String, val commit: String, val remote: Boolean) {

    val local = !remote

    override fun toString() = shortRef

}
