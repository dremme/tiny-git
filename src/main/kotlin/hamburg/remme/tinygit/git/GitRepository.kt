package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.read

private val init = arrayOf("init")
private val clone = arrayOf("clone")
private val revParseHead = arrayOf("rev-parse", "--abbrev-ref", "HEAD")

fun gitInit(path: String) {
    git(*init, path)
}

fun gitClone(repository: Repository, url: String) {
    val response = git(*clone, url, repository.path).trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(setOf(IC, G)))) {
        throw CloneException("Destination '${repository.path}' already exists and is not empty.")
    } else if (response.lines().any { it.contains("$fatalSeparator.*repository.*not found".toRegex(IC)) } ||
            response.lines().any { it.contains("$fatalSeparator.*repository.*does not exist".toRegex(IC)) }) {
        throw CloneException("Repository '$url' not found.")
    }
}

fun gitHead(repository: Repository): String {
    return git(repository, *revParseHead).trim()
}

fun gitMergeMessage(repository: Repository): String {
    return repository.path.asPath().resolve(".git").resolve("MERGE_MSG").read()
}
