package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

private val proxy = arrayOf("config", "http.proxy")
private val unsetProxy = arrayOf("config", "--unset", "http.proxy")

fun gitGetProxy(repository: Repository): String {
    return git(repository, *proxy)
}

fun gitSetProxy(repository: Repository) {
    if (repository.proxyHost.isNotBlank()) git(repository, *proxy, "${repository.proxyHost}:${repository.proxyPort}")
}

fun gitUnsetProxy(repository: Repository) {
    git(repository, *unsetProxy)
}
