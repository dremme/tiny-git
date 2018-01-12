package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.domain.Repository

private val gc = arrayOf("gc", "--aggressive")
private val revParseHead = arrayOf("rev-parse", "--abbrev-ref", "HEAD")
private val branchAll = arrayOf("branch", "--verbose", "--no-abbrev", "--all")
private val checkout = arrayOf("checkout")
private val remotes = "remotes/"
private val errorSeparator = "error: "

fun gitGc(repository: Repository) {
    git(repository, *gc)
}

fun gitHead(repository: Repository): String {
    return git(repository, *revParseHead).trim()
}

fun gitBranchAll(repository: Repository): List<Branch> {
    val branches = mutableListOf<Branch>()
    git(repository, *branchAll) {
        val line = it.substring(2)
        val branch = line.split(" +".toRegex()).first()
        val commitId = line.split(" +".toRegex()).drop(1).first()
        if (!branch.startsWith('(')) branches += Branch(branch.substringAfter(remotes), commitId, branch.startsWith(remotes))
    }
    return branches
}

fun gitCheckout(repository: Repository, branch: String) {
    if (branch == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, branch).trim()
    println(response) // TODO
    if (response.lines().any { it.startsWith(errorSeparator) }) throw CheckoutException()
}

fun gitCheckoutRemote(repository: Repository, branch: String) {
    if (branch.substringAfter('/') == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, "-b", branch.substringAfter('/'), "--track", branch).trim()
    println(response) // TODO
    if (response.lines().any { it.startsWith(errorSeparator) }) throw CheckoutException()
}

fun gitCheckout(repository: Repository, files: List<GitFile>) {
    git(repository, *checkout, "HEAD", "--", *files.map { it.path }.toTypedArray())
}
