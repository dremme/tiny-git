package hamburg.remme.tinygit.git.api

import com.jcraft.jsch.Session
import hamburg.remme.tinygit.gui.decrypt
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig.Host
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS

class GitCredentials(private val sshPath: String, private val username: String, private val password: ByteArray) {

    val isSSH = sshPath.isNotBlank()
    val userCredentials by lazy { UsernamePasswordCredentialsProvider(username, password.decrypt()) }
    val sshTransport by lazy {
        TransportConfigCallback {
            (it as SshTransport).sshSessionFactory = object : JschConfigSessionFactory() {
                override fun createDefaultJSch(fs: FS) = super.createDefaultJSch(fs).also {
                    it.addIdentity(sshPath, password.takeIf { it.isNotEmpty() }?.decrypt())
                }

                override fun configure(host: Host, session: Session) = Unit
            }
        }
    }

}
