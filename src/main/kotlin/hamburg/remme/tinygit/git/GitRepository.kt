package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.read

private val revParseHead = arrayOf("rev-parse", "--abbrev-ref", "HEAD")

fun gitHead(repository: Repository): String {
    return git(repository, *revParseHead).trim()
}

fun gitMergeMessage(repository: Repository): String {
    return repository.path.asPath().resolve(".git").resolve("MERGE_MSG").read()
}
