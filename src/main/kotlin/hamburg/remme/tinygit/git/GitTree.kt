package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.domain.GitStatus
import hamburg.remme.tinygit.domain.Repository

private val status = arrayOf("status", "--porcelain", "--untracked-files=all")
private val lsTree = arrayOf("ls-tree", "--name-only", "--full-tree", "-r", "HEAD")
private val diffTree = arrayOf("diff-tree", "--no-commit-id", "--name-status", "--find-copies", "-r")
private val fileSeparator = " -> "

fun gitStatus(repository: Repository): GitStatus {
    val staged = mutableListOf<GitFile>()
    val pending = mutableListOf<GitFile>()
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
            staged += GitFile(it.toStatusFile(), "", GitFile.Status.CONFLICT, true)
            pending += GitFile(it.toStatusFile(), "", GitFile.Status.CONFLICT, false)
        } else {
            when (first) {
                'M' -> staged += GitFile(it.toStatusFile(), "", GitFile.Status.MODIFIED, true)
                'A' -> staged += GitFile(it.toStatusFile(), "", GitFile.Status.ADDED, true)
                'D' -> staged += GitFile(it.toStatusFile(), "", GitFile.Status.REMOVED, true)
                'R' -> staged += GitFile(it.toStatusFile(), it.toStatusFileOld(), GitFile.Status.RENAMED, true)
                'C' -> staged += GitFile(it.toStatusFile(), it.toStatusFileOld(), GitFile.Status.COPIED, true)
            }
            when (second) {
                'M' -> pending += GitFile(it.toStatusFile(), "", GitFile.Status.MODIFIED, false)
                'D' -> pending += GitFile(it.toStatusFile(), "", GitFile.Status.REMOVED, false)
                '?' -> pending += GitFile(it.toStatusFile(), "", GitFile.Status.ADDED, false)
            }
        }
    }
    return GitStatus(staged, pending)
}

fun gitDiffTree(repository: Repository, commit: Commit): List<GitFile> {
    val entries = mutableListOf<GitFile>()
    git(repository, *diffTree, commit.id) {
        when (it[0]) {
            'A' -> entries += GitFile(it.toDiffFile(), "", GitFile.Status.ADDED, true)
            'R' -> entries += GitFile(it.toDiffFile(), it.toDiffFileOld(), GitFile.Status.RENAMED, true)
            'C' -> entries += GitFile(it.toDiffFile(), it.toDiffFileOld(), GitFile.Status.COPIED, true)
            'M' -> entries += GitFile(it.toDiffFile(), "", GitFile.Status.MODIFIED, true)
            'D' -> entries += GitFile(it.toDiffFile(), "", GitFile.Status.REMOVED, true)
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
