package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository

private val diff = arrayOf("diff", "--find-copies")
private val diffNoIndex = arrayOf("diff", "--no-index", "/dev/null")

fun gitDiff(repository: Repository, file: File, lines: Int): String {
    if (!file.isCached && file.status == File.Status.ADDED) return git(repository, *diffNoIndex, file.path)
    if (file.isCached && file.oldPath.isNotBlank()) return git(repository, *diff, "--unified=$lines", "--cached", "--", file.oldPath, file.path)
    if (file.isCached) return git(repository, *diff, "--unified=$lines", "--cached", "--", file.path) // TODO
    return git(repository, *diff, "--unified=$lines", "--", file.path)
}

fun gitDiff(repository: Repository, file: File, commit: Commit, lines: Int): String {
    if (commit.parents.size > 1) return ""
    if (file.oldPath.isNotBlank()) return git(repository, *diff, "--unified=$lines", commit.parentId, commit.id, "--", file.oldPath, file.path)
    return git(repository, *diff, "--unified=$lines", commit.parentId, commit.id, "--", file.path) // TODO
}
