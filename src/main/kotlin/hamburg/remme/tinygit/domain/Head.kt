package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.git.EMPTY_ID

class Head(
    id: String,
    name: String,
) : Branch(id, name, false) {
    companion object {
        val EMPTY = Head(EMPTY_ID, "/dev/null") // special empty tree id
    }
}
