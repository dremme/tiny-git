package hamburg.remme.tinygit.git.api

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import hamburg.remme.tinygit.decrypt
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.util.FS

class TransportCallback(private val ssh: String, private val password: ByteArray) : TransportConfigCallback {

    override fun configure(transport: Transport) {
        (transport as SshTransport).sshSessionFactory = object : JschConfigSessionFactory() {

            override fun createDefaultJSch(fs: FS): JSch {
                val jsch = super.createDefaultJSch(fs)
                jsch.addIdentity(ssh, password.takeIf { it.isNotEmpty() }?.decrypt())
                return jsch
            }

            override fun configure(host: OpenSshConfig.Host, session: Session) = Unit
        }
    }

}
