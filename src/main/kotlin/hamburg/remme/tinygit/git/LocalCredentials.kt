package hamburg.remme.tinygit.git

import com.jcraft.jsch.Session
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig.Host
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS


class LocalCredentials(var ssh: String = "", var username: String = "", var password: String = "") {

    val userCredentials by lazy { UsernamePasswordCredentialsProvider(username, password) }

    val sshTransport by lazy {
        TransportConfigCallback {
            (it as SshTransport).sshSessionFactory = object : JschConfigSessionFactory() {
                override fun createDefaultJSch(fs: FS) =
                        super.createDefaultJSch(fs).also { it.addIdentity(ssh, password.takeIf { it.isNotBlank() }) }

                override fun configure(host: Host, session: Session) {
                }
            }
        }
    }

    fun isSSH() = ssh.isNotBlank()

}
