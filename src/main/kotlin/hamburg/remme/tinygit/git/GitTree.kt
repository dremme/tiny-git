package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.Status

private val status = arrayOf("status", "--porcelain", "--untracked-files=all")
private val lsTree = arrayOf("ls-tree", "--name-only", "--full-tree", "-r", "HEAD")
private val diffTree = arrayOf("diff-tree", "--no-commit-id", "--name-status", "--find-copies", "-r")
private val fileSeparator = " -> "

fun gitStatus(repository: Repository): Status {
    val staged = mutableListOf<File>()
    val pending = mutableListOf<File>()
    git(repository, *status) {
        val first = it[0]
        val second = it[1]
        // TODO: be more specific about conflicts
        if ((first == 'D' && second == 'D') ||
                (first == 'A' && second == 'U') ||
                (first == 'U' && second == 'D') ||
                (first == 'U' && second == 'A') ||
                (first == 'D' && second == 'U') ||
                (first == 'A' && second == 'A') ||
                (first == 'U' && second == 'U')) {
            staged += File(it.toStatusFile(), "", File.Status.CONFLICT, true)
            pending += File(it.toStatusFile(), "", File.Status.CONFLICT, false)
        } else {
            when (first) {
                'M' -> staged += File(it.toStatusFile(), "", File.Status.MODIFIED, true)
                'A' -> staged += File(it.toStatusFile(), "", File.Status.ADDED, true)
                'D' -> staged += File(it.toStatusFile(), "", File.Status.REMOVED, true)
                'R' -> staged += File(it.toStatusFile(), it.toStatusFileOld(), File.Status.RENAMED, true)
                'C' -> staged += File(it.toStatusFile(), it.toStatusFileOld(), File.Status.COPIED, true)
            }
            when (second) {
                'M' -> pending += File(it.toStatusFile(), "", File.Status.MODIFIED, false)
                'D' -> pending += File(it.toStatusFile(), "", File.Status.REMOVED, false)
                '?' -> pending += File(it.toStatusFile(), "", File.Status.ADDED, false)
            }
        }
    }
    return Status(staged.toList(), pending.toList())
}

fun gitDiffTree(repository: Repository, commit: Commit): List<File> {
    if (commit.parents.size > 1) return emptyList()
    val entries = mutableListOf<File>()
    git(repository, *diffTree, commit.parentId, commit.id) {
        when (it[0]) {
            'A' -> entries += File(it.toDiffFile(), "", File.Status.ADDED, true)
            'R' -> entries += File(it.toDiffFile(), it.toDiffFileOld(), File.Status.RENAMED, true)
            'C' -> entries += File(it.toDiffFile(), it.toDiffFileOld(), File.Status.COPIED, true)
            'M' -> entries += File(it.toDiffFile(), "", File.Status.MODIFIED, true)
            'D' -> entries += File(it.toDiffFile(), "", File.Status.REMOVED, true)
        }
    }
    return entries
}

fun gitLsTree(repository: Repository): List<String> {
    val entries = mutableListOf<String>()
    git(repository, *lsTree) { entries += it }
    return entries
}

private fun String.toStatusFile(): String {
    val name = substring(3).substringAfter(fileSeparator)
    if (name.startsWith('"') && name.endsWith('"')) return name.substring(1, name.length - 1)
    return name
}

private fun String.toStatusFileOld(): String {
    val name = substring(3).substringBefore(fileSeparator)
    if (name.startsWith('"') && name.endsWith('"')) return name.substring(1, name.length - 1)
    return name
}

private fun String.toDiffFile() = split('\t').last()

private fun String.toDiffFileOld() = split('\t').dropLast(1).last()
