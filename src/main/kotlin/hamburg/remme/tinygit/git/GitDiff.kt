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

fun gitDiff(
    repository: Repository,
    file: File,
    lines: Int,
): String {
    if (!file.isCached && file.status == File.Status.ADDED) return git(repository, *diffNoIndex, file.path)
    if (file.isCached) return git(repository, *diff, "--unified=$lines", "--cached", "--", file.oldPath, file.path)
    return git(repository, *diff, "--unified=$lines", "--", file.path)
}

fun gitDiff(
    repository: Repository,
    file: File,
    commit: Commit,
    lines: Int,
): String {
    if (commit.parents.size > 1) return ""
    return git(repository, *diff, "--unified=$lines", commit.parentId, commit.id, "--", file.oldPath, file.path)
}

/** Working tree vs index (`cached = false`) or index vs HEAD (`cached = true`). */
fun gitDiffNumstat(
    repository: Repository,
    cached: Boolean,
): List<NumStat> {
    val numStat = mutableListOf<NumStat>()
    if (cached) {
        git(repository, *diffNumstat, "--cached") { it.appendTo(numStat) }
    } else {
        git(repository, *diffNumstat) { it.appendTo(numStat) }
    }
    return numStat
}

/** Single commit vs its first parent (empty tree for root commits). Merges → empty. */
fun gitDiffNumstat(
    repository: Repository,
    commit: Commit,
): List<NumStat> {
    if (commit.parents.size > 1) return emptyList()
    val numStat = mutableListOf<NumStat>()
    git(repository, *diffNumstat, commit.parentId, commit.id) { it.appendTo(numStat) }
    return numStat
}

/** Range from..to. Same commit → parent..commit so one-commit weeks still yield stats. */
fun gitDiffNumstat(
    repository: Repository,
    from: Commit,
    to: Commit,
): List<NumStat> {
    if (from == to) return gitDiffNumstat(repository, to)
    val numStat = mutableListOf<NumStat>()
    git(repository, *diffNumstat, from.id, to.id) { it.appendTo(numStat) }
    return numStat
}

fun gitBlame(
    repository: Repository,
    path: String,
    after: LocalDate,
): Map<String, Int> {
    val lines = mutableListOf<String>()
    git(repository, *blame, "--after=\"${after.atStartOfDay()}\"", path) {
        lineRegex.matchEntire(it)?.let { lines += it.groupValues[1] }
    }
    return lines.groupingBy { it }.eachCount()
}

private fun String.appendTo(list: MutableList<NumStat>) {
    if (startsWith(WARNING_SEPARATOR) || startsWith(ERROR_SEPARATOR) || startsWith(FATAL_SEPARATOR)) return
    val parts = split('\t')
    if (parts.size < 3) return
    list += NumStat(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0, parts[2])
}
