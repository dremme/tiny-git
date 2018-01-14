package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository

private val branchAll = arrayOf("branch", "--verbose", "--no-abbrev", "--all")
private val checkout = arrayOf("checkout")
private val remotes = "remotes/"

fun gitBranchList(repository: Repository): List<Branch> {
    val branches = mutableListOf<Branch>()
    git(repository, *branchAll) {
        val line = it.substring(2)
        val branch = line.split(" +".toRegex())[0]
        val commitId = line.split(" +".toRegex()).drop(1)[0]
        if (!branch.startsWith('(')) branches += Branch(branch.substringAfter(remotes), commitId, branch.startsWith(remotes))
    }
    return branches
}

fun gitCheckout(repository: Repository, branch: String) {
    if (branch == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, branch).trim()
    if (response.lines().any { it.startsWith(errorSeparator) }) throw CheckoutException()
}

fun gitCheckout(repository: Repository, files: List<File>) {
    git(repository, *checkout, "HEAD", "--", *files.map { it.path }.toTypedArray())
}

fun gitCheckoutRemote(repository: Repository, branch: String) {
    if (branch.substringAfter('/') == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, "-b", branch.substringAfter('/'), "--track", branch).trim()
    if (response.lines().any { it.startsWith(errorSeparator) }) throw CheckoutException()
}
