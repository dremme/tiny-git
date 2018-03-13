package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Head
import hamburg.remme.tinygit.domain.Repository
import java.util.concurrent.atomic.AtomicInteger

private val revParseHead = arrayOf("rev-parse", "--abbrev-ref", "HEAD")
private val branchAll = arrayOf("branch", "--no-abbrev", "--all", "--verbose")
private val branchMove = arrayOf("branch", "--move")
private val branchDelete = arrayOf("branch", "--delete")
private val branchDeleteForce = arrayOf("branch", "--delete", "--force")
private val checkout = arrayOf("checkout")
private val checkoutCreate = arrayOf("checkout", "-b")
private val headCounter = AtomicInteger()
private const val remotes = "remotes/"

fun gitHead(repository: Repository): Head {
    return Head(headCounter.getAndIncrement().toString(), git(repository, *revParseHead).trim())
}

fun gitBranchList(repository: Repository): List<Branch> {
    val branches = mutableListOf<Branch>()
    git(repository, *branchAll) {
        val branchMatch = "[* ] (\\(.+?\\)|.+?) +([\\da-f]+|-> [-./_\\w]+).*".toRegex().matchEntire(it)!!.groupValues
        val branch = branchMatch[1].parseRef()
        val commitId = branchMatch[2]
        if (branch != "${remotes}origin/HEAD") {
            branches += Branch(commitId, branch.substringAfter(remotes), branch.startsWith(remotes))
        }
    }
    return branches
}

fun gitBranch(repository: Repository, name: String) {
    val response = git(repository, *checkoutCreate, name).trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(IC))) throw BranchAlreadyExistsException()
    else if (response.contains("$fatalSeparator.*not a valid branch name".toRegex(IC))) throw BranchNameInvalidException()
}

fun gitBranchMove(repository: Repository, branch: Branch, newName: String) {
    val response = git(repository, *branchMove, branch.name, newName).trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(IC))) throw BranchAlreadyExistsException()
}

fun gitBranchDelete(repository: Repository, branch: Branch, force: Boolean) {
    if (force) {
        git(repository, *branchDeleteForce, branch.name)
    } else {
        val response = git(repository, *branchDelete, branch.name).trim()
        if (response.startsWith(errorSeparator)) throw BranchUnpushedException()
    }
}

fun gitCheckout(repository: Repository, files: List<File>) {
    git(repository, *checkout, "--", *files.map { it.path }.toTypedArray())
}

fun gitCheckout(repository: Repository, branch: Branch) {
    if (branch.name == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, branch.name).trim()
    if (response.startsWith(errorSeparator)) throw CheckoutException()
    else if (response.startsWith(fatalSeparator)) throw CheckoutException()
}

fun gitCheckout(repository: Repository, commit: Commit) {
    val response = git(repository, *checkout, commit.id).trim()
    if (response.startsWith(errorSeparator)) throw CheckoutException()
    else if (response.startsWith(fatalSeparator)) throw CheckoutException()
}

fun gitCheckoutRemote(repository: Repository, branch: Branch) {
    if (branch.name.substringAfter('/') == "HEAD") return // cannot checkout HEAD directly
    val response = git(repository, *checkout, "-b", branch.name.substringAfter('/'), "--track", branch.name).trim()
    if (response.startsWith(errorSeparator)) throw CheckoutException()
    else if (response.startsWith(fatalSeparator)) throw CheckoutException()
}

private fun String.parseRef() = if (matches("\\(HEAD detached at [\\da-f]+\\)".toRegex())) "HEAD" else this
