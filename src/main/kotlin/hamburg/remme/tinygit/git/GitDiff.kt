package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.domain.Repository

private val diff = arrayOf("diff")
private val diffNoIndex = arrayOf("diff", "--no-index", "/dev/null")

fun gitDiff(repository: Repository, file: GitFile, lines: Int): String {
    if (!file.cached && file.status == GitFile.Status.ADDED) return git(repository, *diffNoIndex, file.path)
    if (file.cached) return git(repository, *diff, "--unified=$lines", "--cached", "--", file.oldPath, file.path)
    return git(repository, *diff, "--unified=$lines", file.path)
}

fun gitDiff(repository: Repository, file: GitFile, commit: Commit, lines: Int): String {
    if (commit.parents.isEmpty()) return ""
    return git(repository, *diff, "--unified=$lines", commit.parents[0], commit.id, "--", file.oldPath, file.path)
}
