package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Credentials

private val credentialWincredGet = arrayOf("credential-wincred", "get")
private val credentialWincredStore = arrayOf("credential-wincred", "store")
private val credentialKeychainGet = arrayOf("credential-osxkeychain", "get")
private val credentialKeychainStore = arrayOf("credential-osxkeychain", "store")
private const val usernamePrefix = "username="
private const val passwordPrefix = "password="
private const val hostPrefix = "host="
private const val protocolPrefix = "protocol="

fun gitCredentialWincredGet(host: String, protocol: String) = gitCredentialGet(host, protocol, *credentialWincredGet)

fun gitCredentialWincredStore(credentials: Credentials) = gitCredentialStore(credentials, *credentialWincredStore)

fun gitCredentialKeychainGet(host: String, protocol: String) = gitCredentialGet(host, protocol, *credentialKeychainGet)

fun gitCredentialKeychainStore(credentials: Credentials) = gitCredentialStore(credentials, *credentialKeychainStore)

private fun gitCredentialGet(host: String, protocol: String, vararg args: String): Credentials {
    var username = ""
    var password = ""
    git(arrayOf("$hostPrefix$host", "$protocolPrefix$protocol", "\n"), *args) {
        if (it.startsWith(usernamePrefix)) username = it.substringAfter(usernamePrefix)
        else if (it.startsWith(passwordPrefix)) password = it.substringAfter(passwordPrefix)
    }
    return Credentials(username, password, host, protocol)
}

private fun gitCredentialStore(credentials: Credentials, vararg args: String) {
    git(arrayOf("$hostPrefix${credentials.host}",
            "$protocolPrefix${credentials.protocol}",
            "$usernamePrefix${credentials.username}",
            "$passwordPrefix${credentials.password}",
            "\n"),
            *args)
}
