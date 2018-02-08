package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.atEndOfDay
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.localDateTime
import java.time.LocalDate
import java.time.LocalDateTime

private const val idSeparator = "id: "
private const val parentsSeparator = "parents: "
private const val refsSeparator = "refs: "
private const val nameSeparator = "name: "
private const val mailSeparator = "mail: "
private const val dateSeparator = "date: "
private const val bodySeparator = "body: "
private const val eom = "<eom>"
private const val logFormat = "--pretty=format:$idSeparator%H%n$parentsSeparator%P%n$refsSeparator%d%n$nameSeparator%cn%n$mailSeparator%ce%n$dateSeparator%cd%n$bodySeparator%B%n$eom"
private val log1 = arrayOf("log", "-1", "--pretty=%B")
private val logAll = arrayOf("log", "--branches", "--remotes", "--tags", "--date=raw", logFormat)
private val logNot = arrayOf("log", "HEAD", "--date=raw", logFormat, "--not")
private val revlistCount = arrayOf("rev-list", "--count")
private val revlistCountNot = arrayOf("rev-list", "--count", "HEAD", "--not")
private const val headSeparator = " -> "

fun gitHeadMessage(repository: Repository): String {
    return git(repository, *log1)
}

fun gitLog(repository: Repository, skip: Int, maxCount: Int): List<Commit> {
    val parser = CommitParser()
    git(repository, *logAll, "--skip=$skip", "--max-count=$maxCount") { parser.parseLine(it) }
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
        return Divergence(gitDivergenceExclusive(repository), 0)
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

class CommitParser {

    val commits = mutableListOf<Commit>()
    private var id = ""
    private val parents = mutableListOf<String>()
    private val refs = mutableListOf<String>()
    private var fullMessage = ""
    private var date = LocalDateTime.now()!!
    private var authorName = ""
    private var authorMail = ""
    private var messageBuilder: StringBuilder? = null

    fun parseLine(line: String) {
        when {
            messageBuilder != null && line != eom -> messageBuilder!!.appendln(line)
            line.startsWith(idSeparator) -> id = line.substringAfter(idSeparator)
            line.startsWith(parentsSeparator) -> parents += line.substringAfter(parentsSeparator).split(' ').filter { it.isNotBlank() }
            line.startsWith(refsSeparator) -> refs += line.parseRefs()
            line.startsWith(dateSeparator) -> date = line.substringAfter(dateSeparator).parseDate()
            line.startsWith(nameSeparator) -> authorName = line.substringAfterLast(nameSeparator)
            line.startsWith(mailSeparator) -> authorMail = line.substringAfterLast(mailSeparator)
            line.startsWith(bodySeparator) -> messageBuilder = StringBuilder(line.substringAfterLast(bodySeparator)).appendln()
            line == eom -> {
                fullMessage = messageBuilder.toString()
                commits += Commit(id, parents.toList(), refs.toList(), fullMessage, date, authorName, authorMail)
                reset()
            }
        }
    }

    private fun reset() {
        id = ""
        parents.clear()
        refs.clear()
        fullMessage = ""
        date = LocalDateTime.now()
        authorName = ""
        authorMail = ""
        messageBuilder = null
    }

    private fun String.parseRefs(): List<String> {
        return substringAfter(refsSeparator)
                .trim()
                .replace("[()]".toRegex(), "")
                .split(',')
                .filter { it.isNotBlank() }
                .map { it.substringAfter(headSeparator).trim() }
    }

    // TODO: local time is being ignored
    private fun String.parseDate(): LocalDateTime {
        val match = "(\\d+) [-+](\\d{2})(\\d{2})".toRegex().matchEntire(this)!!.groupValues
        return localDateTime(match[1].toLong())
    }

}
