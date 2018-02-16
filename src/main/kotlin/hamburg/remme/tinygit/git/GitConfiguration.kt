package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Repository

// TODO: maybe do not configure globally?
private val credentialHelper = arrayOf("config", "--global", "credential.helper")
private val userName = arrayOf("config", "user.name")
private val userEmail = arrayOf("config", "user.email")
private val proxy = arrayOf("config", "http.proxy")
private val unsetProxy = arrayOf("config", "--unset", "http.proxy")

fun gitGetCredentialHelper(): String {
    return git(*credentialHelper).trim()
}

fun gitSetWincred() {
    git(*credentialHelper, "wincred")
}

fun gitSetKeychain() {
    git(*credentialHelper, "osxkeychain")
}

fun gitGetUserName(repository: Repository): String {
    return git(repository, *userName).trim()
}

fun gitSetUserName(repository: Repository, name: String) {
    git(repository, *userName, name)
}

fun gitGetUserEmail(repository: Repository): String {
    return git(repository, *userEmail).trim()
}

fun gitSetUserEmail(repository: Repository, email: String) {
    git(repository, *userEmail, email)
}

fun gitGetProxy(repository: Repository): String {
    return git(repository, *proxy).trim()
}

fun gitSetProxy(repository: Repository, hostPort: String) {
    git(repository, *proxy, hostPort)
}

fun gitUnsetProxy(repository: Repository) {
    git(repository, *unsetProxy)
}
