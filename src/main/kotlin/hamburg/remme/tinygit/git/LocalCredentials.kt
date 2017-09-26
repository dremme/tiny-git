package hamburg.remme.tinygit.git

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

class LocalCredentials(var ssh: String = "", var username: String = "", var password: String = "") {

    fun toCredentialsProvider() = UsernamePasswordCredentialsProvider(username, password)

}
