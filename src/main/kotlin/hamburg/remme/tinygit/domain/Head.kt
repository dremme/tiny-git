package hamburg.remme.tinygit.domain

class Head(id: String, name: String) : Branch(id, name, false) {

    companion object {
        val EMPTY = Head("4b825dc642cb6eb9a060e54bf8d69288fbee4904", "/dev/null") // special empty tree id
    }

}
