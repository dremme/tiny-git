package hamburg.remme.tinygit.system

internal class ConsoleCollector {

    val lines: List<String> = arrayListOf()

    fun collect(line: String) {
        lines as ArrayList<String> += line
    }

    override fun toString(): String {
        return lines.joinToString("\n")
    }

}
