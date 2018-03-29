package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.atEndOfDay
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.CommitIsh
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.localDateTime
import java.time.LocalDate
import java.time.LocalDateTime

private const val idSeparator = "id: "
private const val parentsSeparator = "parents: "
private const val nameSeparator = "name: "
private const val mailSeparator = "mail: "
private const val dateSeparator = "date: "
private const val bodySeparator = "body: "
private const val eom = "<eom>"
private const val logFormat = "--pretty=format:$idSeparator%H%n$parentsSeparator%P%n$nameSeparator%cn%n$mailSeparator%ce%n$dateSeparator%cd%n$bodySeparator%B%n$eom"
private val log1 = arrayOf("log", "-1", "--pretty=%B")
private val log = arrayOf("log", "--first-parent", "--date=raw", logFormat)
private val logAll = arrayOf("log", "--branches", "--remotes", "--tags", "--date=raw", logFormat)
private val logNot = arrayOf("log", "HEAD", "--date=raw", logFormat, "--not")
private val revlistCount = arrayOf("rev-list", "--count")
private val revlistCountNot = arrayOf("rev-list", "--count", "HEAD", "--not")

fun gitHeadMessage(repository: Repository): String {
    return git(repository, *log1)
}

fun gitLog(repository: Repository, all: Boolean, noMerges: Boolean, skip: Int, maxCount: Int): List<Commit> {
    val parser = CommitParser()
    git(repository, *if (all) logAll else log, if (noMerges) "--no-merges" else "", "--skip=$skip", "--max-count=$maxCount") { parser.parseLine(it) }
    return parser.commits
}

fun gitLog(repository: Repository, after: LocalDate, before: LocalDate): List<Commit> {
    val parser = CommitParser()
    git(repository, *logAll, "--after=\"${after.atStartOfDay()}\"", "--before=\"${before.atEndOfDay()}\"") { parser.parseLine(it) }
    return parser.commits
}

fun gitLogExclusive(repository: Repository): List<Commit> {
    val parser = CommitParser()
    git(repository, *logNot, *excludeDefault(repository)) { parser.parseLine(it) }
    return parser.commits
}

fun gitDivergence(repository: Repository): Divergence {
    val head = gitHead(repository)
    val response = git(repository, *revlistCount, "origin/$head..$head")
    if (response.startsWith(fatalSeparator)) {
        val ahead = if (defaultBranches.contains(head.name)) git(repository, *revlistCount, head.name).lines()[0].toInt() else gitDivergenceExclusive(repository)
        return Divergence(ahead, 0)
    }
    val ahead = response.lines()[0].toInt()
    val behind = git(repository, *revlistCount, "$head..origin/$head").lines()[0].toInt()
    return Divergence(ahead, behind)
}

fun gitDivergenceExclusive(repository: Repository): Int {
    return git(repository, *revlistCountNot, *excludeDefault(repository)).lines()[0].toInt()
}

private fun excludeDefault(repository: Repository): Array<String> {
    val branches = gitBranchList(repository).map { it.name }
    return defaultBranches.filter { branches.contains(it) }.toTypedArray()
}

private class CommitParser {

    val commits = mutableListOf<Commit>()
    private var builder = CommitBuilder()
    private var messageBuilder: StringBuilder? = null

    fun parseLine(line: String) {
        when {
            messageBuilder != null && line != eom -> messageBuilder!!.appendln(line)
            line.startsWith(idSeparator) -> builder.id = line.substringAfter(idSeparator)
            line.startsWith(parentsSeparator) -> builder.parents += line.substringAfter(parentsSeparator).split(' ').filter { it.isNotBlank() }
            line.startsWith(dateSeparator) -> builder.date = line.substringAfter(dateSeparator).parseDate()
            line.startsWith(nameSeparator) -> builder.authorName = line.substringAfterLast(nameSeparator)
            line.startsWith(mailSeparator) -> builder.authorMail = line.substringAfterLast(mailSeparator)
            line.startsWith(bodySeparator) -> messageBuilder = StringBuilder(line.substringAfterLast(bodySeparator)).appendln()
            line == eom -> {
                builder.fullMessage = messageBuilder.toString()
                commits += builder.build()
                builder = CommitBuilder()
                messageBuilder = null
            }
        }
    }

    // TODO: local time is being ignored
    private fun String.parseDate(): LocalDateTime {
        val match = "(\\d+) [-+](\\d{2})(\\d{2})".toRegex().matchEntire(this)!!.groupValues
        return localDateTime(match[1].toLong())
    }

}

private class CommitBuilder {

    lateinit var id: String
    val parents = mutableListOf<String>()
    lateinit var fullMessage: String
    lateinit var date: LocalDateTime
    lateinit var authorName: String
    lateinit var authorMail: String

    fun build() = Commit(id, parents.map { CommitIsh(it) }, fullMessage, date, authorName, authorMail)

}
