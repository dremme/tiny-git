package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.atEndOfDay
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

private val idSeparator = "id: "
private val parentsSeparator = "parents: "
private val nameSeparator = "name: "
private val mailSeparator = "mail: "
private val dateSeparator = "date: "
private val bodySeparator = "body: "
private val eom = "<eom>"
private val log1 = arrayOf("log", "-1", "--pretty=%B")
private val logAll = arrayOf("log", "--all", "--date=raw", "--pretty=format:$idSeparator%H%n$parentsSeparator%P%n$nameSeparator%an%n$mailSeparator%ae%n$dateSeparator%ad%n$bodySeparator%B%n$eom")

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

private fun String.abbreviate() = substring(0, 8)

private fun String.parseDate(): LocalDateTime {
    val match = "(\\d+) [-+](\\d{2})(\\d{2})".toRegex().matchEntire(this)!!.groupValues
    return LocalDateTime.ofEpochSecond(match[1].toLong(), 0, ZoneOffset.ofHoursMinutes(match[2].toInt(), match[3].toInt()))
}

class CommitParser {

    val commits = mutableListOf<Commit>()
    private var id = ""
    private val parents = mutableListOf<String>()
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
            line.startsWith(dateSeparator) -> date = line.substringAfter(dateSeparator).parseDate()
            line.startsWith(nameSeparator) -> authorName = line.substringAfterLast(nameSeparator)
            line.startsWith(mailSeparator) -> authorMail = line.substringAfterLast(mailSeparator)
            line.startsWith(bodySeparator) -> messageBuilder = StringBuilder(line.substringAfterLast(bodySeparator)).appendln()
            line == eom -> {
                fullMessage = messageBuilder.toString()
                commits += Commit(
                        id, id.abbreviate(),
                        parents.toList(), parents.map { it.abbreviate() },
                        fullMessage, fullMessage.lines()[0],
                        date, authorName, authorMail)
                reset()
            }
        }
    }

    private fun reset() {
        id = ""
        parents.clear()
        fullMessage = ""
        date = LocalDateTime.now()
        authorName = ""
        authorMail = ""
        messageBuilder = null
    }

}
