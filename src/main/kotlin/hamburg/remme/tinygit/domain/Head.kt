package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.git.emptyId

class Head(id: String, name: String) : Branch(id, name, false) {

    companion object {
        val EMPTY = Head(emptyId, "/dev/null") // special empty tree id
    }

}
