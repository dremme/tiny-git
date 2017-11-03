package hamburg.remme.tinygit.git

class LocalBranch(val shortRef: String, val commit: String, val current: Boolean, val remote: Boolean) {

    override fun toString() = shortRef

}
