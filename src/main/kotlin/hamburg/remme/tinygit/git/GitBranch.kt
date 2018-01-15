package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository

private val branchAll = arrayOf("branch", "--verbose", "--no-abbrev", "--all")
private val branchMove = arrayOf("branch", "--move")
private val branchDelete = arrayOf("branch", "--delete")
private val branchDeleteForce = arrayOf("branch", "--delete", "--force")
private val checkout = arrayOf("checkout")
private val checkoutCreate = arrayOf("checkout", "-b")
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

fun gitBranch(repository: Repository, name: String) {
    val response = git(repository, *checkoutCreate, name).trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(IC))) throw BranchAlreadyExistsException()
    else if (response.contains("$fatalSeparator.*not a valid branch name".toRegex(IC))) throw BranchNameInvalidException()
}

fun gitBranchMove(repository: Repository, branch: String, newName: String) {
    val response = git(repository, *branchMove, branch, newName).trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(IC))) throw BranchAlreadyExistsException()
}

fun gitBranchDelete(repository: Repository, branch: String) {
    val response = git(repository, *branchDelete, branch).trim()
    if (response.startsWith(errorSeparator)) throw BranchUnpushedException()
}

fun gitBranchDeleteForce(repository: Repository, branch: String) {
    git(repository, *branchDeleteForce, branch)
}

fun gitCheckout(repository: Repository, files: List<File>) {
    git(repository, *checkout, "HEAD", "--", *files.map { it.path }.toTypedArray())
}

fun gitCheckout(repository: Repository, branch: String) {
    if (branch == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, branch).trim()
    if (response.startsWith(errorSeparator)) throw CheckoutException()
    else if (response.startsWith(fatalSeparator)) throw CheckoutException()
}

fun gitCheckoutRemote(repository: Repository, branch: String) {
    if (branch.substringAfter('/') == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, "-b", branch.substringAfter('/'), "--track", branch).trim()
    if (response.startsWith(errorSeparator)) throw CheckoutException()
    else if (response.startsWith(fatalSeparator)) throw CheckoutException()
}
