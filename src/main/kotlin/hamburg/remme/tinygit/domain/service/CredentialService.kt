package hamburg.remme.tinygit.domain.service

import com.sun.javafx.PlatformUtil
import hamburg.remme.tinygit.domain.Credentials
import hamburg.remme.tinygit.git.gitCredentialGet
import hamburg.remme.tinygit.git.gitCredentialStore

class CredentialService {

    lateinit var credentialHandler: (String) -> Credentials?

    // TODO: refactor this to beauty
    // TODO: throw exception if remote doesn't start with http
    fun applyCredentials(remote: String) {
        if (PlatformUtil.isMac() && remote.isNotBlank() && remote.startsWith("http")) {
            val remoteMatch = "(https?)://(.+@)?(.+\\..+?)/.+".toRegex().matchEntire(remote)!!.groupValues
            val dummy = gitCredentialGet(remoteMatch[3], remoteMatch[1])
            if (dummy.isEmpty) credentialHandler.invoke(dummy.host)?.let {
                gitCredentialStore(Credentials(it.username, it.password, dummy.host, dummy.protocol))
            }
        }
    }

}
