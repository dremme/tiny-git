package hamburg.remme.tinygit.domain

class LocalDivergence(val ahead: Int, val behind: Int) {

    operator fun component1() = ahead

    operator fun component2() = behind

}
