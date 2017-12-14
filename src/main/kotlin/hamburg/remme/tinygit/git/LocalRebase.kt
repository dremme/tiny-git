package hamburg.remme.tinygit.git

class LocalRebase(val next: Int, val last: Int) {

    operator fun component1() = next

    operator fun component2() = last

}
