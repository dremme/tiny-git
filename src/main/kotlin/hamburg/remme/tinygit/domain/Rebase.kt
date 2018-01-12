package hamburg.remme.tinygit.domain

class Rebase(val next: Int, val last: Int) {

    operator fun component1() = next

    operator fun component2() = last

}
