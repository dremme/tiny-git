package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository

private val diff = arrayOf("diff", "--find-copies")
private val diffNoIndex = arrayOf("diff", "--no-index", "/dev/null")
private val diffNumstat = arrayOf("diff", "--numstat")

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
    git(repository, *diffNumstat, from.id, to.id) { if (!it.startsWith("warning: ")) numStat += it.parseStat() }
    return numStat
}

private fun String.parseStat(): NumStat {
    val line = split("\t+".toRegex())
    return if (line[0] == "-") NumStat(0, 0, line[2])
    else NumStat(line[0].toInt(), line[1].toInt(), line[2])
}
