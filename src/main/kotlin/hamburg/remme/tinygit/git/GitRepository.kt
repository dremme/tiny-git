package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.read

private val init = arrayOf("init")
private val clone = arrayOf("clone")

fun gitInit(path: String) {
    git(*init, path)
}

fun gitClone(repository: Repository, url: String) {
    val response = if (repository.proxyHost.isBlank()) {
        git(*clone, url, repository.path)
    } else {
        git(*clone, "--config", "http.proxy=${repository.proxyHost}:${repository.proxyPort}", url, repository.path)
    }.trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(setOf(IC, G)))) {
        throw CloneException("Destination '${repository.path}' already exists and is not empty.")
    } else if (response.lines().any { it.contains("$fatalSeparator.*repository.*not found".toRegex(IC)) } ||
            response.lines().any { it.contains("$fatalSeparator.*repository.*does not exist".toRegex(IC)) }) {
        throw CloneException("Repository '$url' not found.")
    }
}

fun gitMergeMessage(repository: Repository): String {
    return repository.path.asPath().resolve(".git").resolve("MERGE_MSG").read()
}
