package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.domain.Credentials

private val credentialWincredGet = arrayOf("credential-wincred", "get")
private val credentialWincredStore = arrayOf("credential-wincred", "store")
private val credentialKeychainGet = arrayOf("credential-osxkeychain", "get")
private val credentialKeychainStore = arrayOf("credential-osxkeychain", "store")
private const val USERNAME_PREFIX = "username="
private const val PASSWORD_PREFIX = "password="
private const val HOST_PREFIX = "host="
private const val PROTOCOL_PREFIX = "protocol="

fun gitCredentialWincredGet(
    host: String,
    protocol: String,
) = gitCredentialGet(host, protocol, *credentialWincredGet)

fun gitCredentialWincredStore(credentials: Credentials) = gitCredentialStore(credentials, *credentialWincredStore)

fun gitCredentialKeychainGet(
    host: String,
    protocol: String,
) = gitCredentialGet(host, protocol, *credentialKeychainGet)

fun gitCredentialKeychainStore(credentials: Credentials) = gitCredentialStore(credentials, *credentialKeychainStore)

private fun gitCredentialGet(
    host: String,
    protocol: String,
    vararg args: String,
): Credentials {
    var username = ""
    var password = ""
    git(arrayOf("$HOST_PREFIX$host", "$PROTOCOL_PREFIX$protocol", "\n"), *args) {
        if (it.startsWith(USERNAME_PREFIX)) {
            username = it.substringAfter(USERNAME_PREFIX)
        } else if (it.startsWith(PASSWORD_PREFIX)) {
            password = it.substringAfter(PASSWORD_PREFIX)
        }
    }
    return Credentials(username, password, host, protocol)
}

private fun gitCredentialStore(
    credentials: Credentials,
    vararg args: String,
) {
    git(
        arrayOf(
            "$HOST_PREFIX${credentials.host}",
            "$PROTOCOL_PREFIX${credentials.protocol}",
            "$USERNAME_PREFIX${credentials.username}",
            "$PASSWORD_PREFIX${credentials.password}",
            "\n",
        ),
        *args,
    )
}
