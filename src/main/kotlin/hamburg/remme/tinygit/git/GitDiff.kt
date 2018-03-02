package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository
import java.time.LocalDate

private val diff = arrayOf("diff", "--find-copies")
private val diffNoIndex = arrayOf("diff", "--no-index", "/dev/null")
private val diffNumstat = arrayOf("diff", "--numstat", "--no-renames")
private val blame = arrayOf("blame", "--show-email")
private val lineRegex = "\\^?[\\da-f]+\\W\\(<(.+?)>.+\\).+".toRegex()

fun gitDiff(repository: Repository, file: File, lines: Int): String {
    if (!file.isCached && file.status == File.Status.ADDED) return git(repository, *diffNoIndex, file.path)
    if (file.isCached) return git(repository, *diff, "--unified=$lines", "--cached", "--", file.oldPath, file.path)
    return git(repository, *diff, "--unified=$lines", "--", file.path)
}

fun gitDiff(repository: Repository, file: File, commit: Commit, lines: Int): String {
    if (commit.parents.size > 1) return ""
    return git(repository, *diff, "--unified=$lines", commit.parentId, commit.id, "--", file.oldPath, file.path)
}

fun gitDiffNumstat(repository: Repository, from: Commit, to: Commit): List<NumStat> {
    val numStat = mutableListOf<NumStat>()
    git(repository, *diffNumstat, if (from != to) from.id else "", to.id) { numStat += it.parseStat() }
    return numStat
}

fun gitBlame(repository: Repository, path: String, after: LocalDate): Map<String, Int> {
    val lines = mutableListOf<String>()
    git(repository, *blame, "--after=\"${after.atStartOfDay()}\"", path) {
        lineRegex.matchEntire(it)?.let { lines += it.groupValues[1] }
    }
    return lines.groupingBy { it }.eachCount()
}

private fun String.parseStat(): NumStat {
    val line = split('\t')
    return if (line[0] == "-") NumStat(0, 0, line[2])
    else NumStat(line[0].toInt(), line[1].toInt(), line[2])
}
