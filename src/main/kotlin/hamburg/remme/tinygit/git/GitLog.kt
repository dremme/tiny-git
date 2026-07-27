package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.CommitIsh
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.localDateTime
import java.time.LocalDate
import java.time.LocalDateTime

private const val ID_SEPARATOR = "id: "
private const val PARENTS_SEPARATOR = "parents: "
private const val NAME_SEPARATOR = "name: "
private const val MAIL_SEPARATOR = "mail: "
private const val DATE_SEPARATOR = "date: "
private const val BODY_SEPARATOR = "body: "
private const val EOM = "<eom>"
private const val LOG_FORMAT =
    "--pretty=format:$ID_SEPARATOR%H%n$PARENTS_SEPARATOR%P%n$NAME_SEPARATOR%cn%n" +
        "$MAIL_SEPARATOR%ce%n$DATE_SEPARATOR%cd%n$BODY_SEPARATOR%B%n$EOM"
private val log1 = arrayOf("log", "-1", "--pretty=%B")
private val log = arrayOf("log", "--first-parent", "--date=raw", LOG_FORMAT)
private val logAll = arrayOf("log", "--branches", "--remotes", "--tags", "--date=raw", LOG_FORMAT)
private val logNot = arrayOf("log", "HEAD", "--date=raw", LOG_FORMAT, "--not")
private val logYears = arrayOf("log", "--branches", "--remotes", "--tags", "--format=%cd", "--date=format:%Y")
private val revlistCount = arrayOf("rev-list", "--count")
private val revlistCountNot = arrayOf("rev-list", "--count", "HEAD", "--not")

fun gitHeadMessage(repository: Repository): String = git(repository, *log1)

fun gitLog(
    repository: Repository,
    all: Boolean,
    noMerges: Boolean,
    skip: Int,
    maxCount: Int,
): List<Commit> {
    val parser = CommitParser()
    git(repository, *if (all) logAll else log, if (noMerges) "--no-merges" else "", "--skip=$skip", "--max-count=$maxCount") {
        parser.parseLine(it)
    }
    return parser.commits
}

fun gitLog(
    repository: Repository,
    after: LocalDate,
    before: LocalDate,
): List<Commit> {
    val parser = CommitParser()
    // --until is exclusive at midnight; +1 day includes the full `before` date.
    git(repository, *logAll, "--since=$after", "--until=${before.plusDays(1)}") { parser.parseLine(it) }
    return parser.commits
}

fun gitLogExclusive(repository: Repository): List<Commit> {
    val parser = CommitParser()
    git(repository, *logNot, *excludeDefault(repository)) { parser.parseLine(it) }
    return parser.commits
}

fun gitLogYears(repository: Repository): List<Int> {
    val years = linkedSetOf<Int>()
    git(repository, *logYears) { it.trim().toIntOrNull()?.let { year -> years += year } }
    return years.sortedDescending()
}

fun gitDivergence(repository: Repository): Divergence {
    val head = gitHead(repository)
    val response = git(repository, *revlistCount, "origin/$head..$head")
    if (response.startsWith(FATAL_SEPARATOR)) {
        val ahead =
            if (defaultBranches.contains(
                    head.name,
                )
            ) {
                git(repository, *revlistCount, head.name).lines()[0].toInt()
            } else {
                gitDivergenceExclusive(repository)
            }
        return Divergence(ahead, 0)
    }
    val ahead = response.lines()[0].toInt()
    val behind = git(repository, *revlistCount, "$head..origin/$head").lines()[0].toInt()
    return Divergence(ahead, behind)
}

fun gitDivergenceExclusive(repository: Repository): Int = git(repository, *revlistCountNot, *excludeDefault(repository)).lines()[0].toInt()

private fun excludeDefault(repository: Repository): Array<String> {
    val branches = gitBranchList(repository).map { it.name }
    return defaultBranches.filter { branches.contains(it) }.toTypedArray()
}

private class CommitParser {
    val commits = mutableListOf<Commit>()
    private var builder = CommitBuilder()

    fun parseLine(line: String) {
        when {
            builder.fullMessage != null && line != EOM -> builder.fullMessage!!.appendLine(line)
            line == EOM -> {
                commits += builder.build()
                builder = CommitBuilder()
            }
            line.startsWith(ID_SEPARATOR) -> builder.id = line.substringAfter(ID_SEPARATOR)
            line.startsWith(PARENTS_SEPARATOR) ->
                builder.parents +=
                    line.substringAfter(PARENTS_SEPARATOR).split(' ').filter { it.isNotBlank() }
            line.startsWith(DATE_SEPARATOR) -> builder.date = line.substringAfter(DATE_SEPARATOR).parseDate()
            line.startsWith(NAME_SEPARATOR) -> builder.authorName = line.substringAfterLast(NAME_SEPARATOR)
            line.startsWith(MAIL_SEPARATOR) -> builder.authorMail = line.substringAfterLast(MAIL_SEPARATOR)
            line.startsWith(BODY_SEPARATOR) -> builder.fullMessage = StringBuilder(line.substringAfterLast(BODY_SEPARATOR)).appendLine()
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
    var fullMessage: StringBuilder? = null
    lateinit var date: LocalDateTime
    lateinit var authorName: String
    lateinit var authorMail: String

    fun build() = Commit(id, parents.map { CommitIsh(it) }, fullMessage.toString(), date, authorName, authorMail)
}
