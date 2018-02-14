package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Credentials

private val credentialGet = arrayOf("credential-osxkeychain", "get")
private val credentialStore = arrayOf("credential-osxkeychain", "store")
private const val usernamePrefix = "username="
private const val passwordPrefix = "password="
private const val hostPrefix = "host="
private const val protocolPrefix = "protocol="

fun gitCredentialGet(host: String, protocol: String): Credentials {
    var username = ""
    var password = ""
    git(arrayOf("$hostPrefix$host", "$protocolPrefix$protocol", "\n"), *credentialGet) {
        if (it.startsWith(usernamePrefix)) username = it.substringAfter(usernamePrefix)
        else if (it.startsWith(passwordPrefix)) password = it.substringAfter(passwordPrefix)
    }
    return Credentials(username, password, host, protocol)
}

fun gitCredentialStore(credentials: Credentials) {
    git(arrayOf("$hostPrefix${credentials.host}",
            "$protocolPrefix${credentials.protocol}",
            "$usernamePrefix${credentials.username}",
            "$passwordPrefix${credentials.password}",
            "\n"),
            *credentialStore)
}
