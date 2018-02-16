package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.read

private val init = arrayOf("init")
private val clone = arrayOf("clone")
private val notFound = "$fatalSeparator.*repository.*not found".toRegex(IC)
private val notExist = "$fatalSeparator.*repository.*does not exist".toRegex(IC)

fun gitInit(path: String) {
    git(*init, path)
}

fun gitClone(repository: Repository, proxy: String, url: String) {
    val response = if (proxy.isBlank()) {
        git(*clone, url, repository.path)
    } else {
        git(*clone, "--config", "http.proxy=$proxy", url, repository.path)
    }.trim()
    if (response.contains("$fatalSeparator.*already exists".toRegex(setOf(IC, G)))) {
        throw CloneException("Destination '${repository.path}' already exists and is not empty.")
    } else if (response.lines().any { it.contains(notFound) || it.contains(notExist) }) {
        throw CloneException("Repository '$url' not found.")
    } else if (!repository.path.asPath().exists()) {
        throw CloneException("Repository '$url' could not be cloned. Unknown error.")
    }
}

fun gitMergeMessage(repository: Repository): String {
    return repository.path.asPath().resolve(".git").resolve("MERGE_MSG").read()
}
