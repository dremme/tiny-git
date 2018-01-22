package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.decrypt
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.encrypt
import hamburg.remme.tinygit.git.gitAddRemote
import hamburg.remme.tinygit.git.gitGetUrl
import hamburg.remme.tinygit.git.gitHasRemote
import hamburg.remme.tinygit.git.gitRemoveRemote
import hamburg.remme.tinygit.git.gitSetProxy
import hamburg.remme.tinygit.git.gitSetPushUrl
import hamburg.remme.tinygit.git.gitSetUrl
import hamburg.remme.tinygit.git.gitUnsetProxy
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.fileChooser
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.passwordField
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.component.Icons
import javafx.scene.control.Label
import javafx.stage.Window

// TODO: should be wider
class SettingsDialog(repository: Repository, window: Window) : Dialog<Unit>(window, "Repository Settings") {

    private val originalUrl = gitGetUrl(repository)
    private val originalHost = repository.proxyHost
    private val originalPort = repository.proxyPort

    init {
        +DialogButton(DialogButton.OK)
        +DialogButton(DialogButton.CANCEL)

        val remote = textField {
            columnSpan(3)
            isEditable = true
            text = originalUrl
        }
        val location = textField {
            columnSpan(3)
            isEditable = false
            text = repository.path
        }
        val ssh = textField {
            text = repository.ssh
        }
        val sshSearch = button {
            columnSpan(2)
            fillWidth()
            graphic = Icons.search()
            maxWidth = Double.MAX_VALUE
            setOnAction { fileChooser(dialogWindow, "Choose a SSH Key") { ssh.text = it.toString() } }
        }
        val username = textField {
            columnSpan(3)
            text = repository.username
        }
        val password = passwordField {
            columnSpan(3)
            text = repository.password.decrypt()
        }
        val proxyHost = textField { text = originalHost }
        val proxyPort = textField {
            prefColumnCount = 4
            intFormatter(originalPort)
        }

        okAction = {
            repository.ssh = ssh.text
            repository.username = username.text
            repository.password = password.text.encrypt()

            val url = remote.text
            if (url != originalUrl) {
                if (url.isNotBlank()) {
                    if (gitHasRemote(repository)) {
                        gitSetUrl(repository, url)
                        gitSetPushUrl(repository, url)
                    } else gitAddRemote(repository, url)
                } else gitRemoveRemote(repository)
            }

            val host = proxyHost.text
            val port = proxyPort.text.toInt()
            if (host != originalHost || port != originalPort) {
                repository.proxyHost = host
                repository.proxyPort = port
                if (host.isNotBlank()) gitSetProxy(repository)
                else gitUnsetProxy(repository)
            }

            State.fireRefresh()
        }
        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Remote:"), remote,
                    Label("Location:"), location,
                    Label("SSH Key:"), ssh, sshSearch,
                    Label("User:"), username,
                    Label("Password:"), password,
                    Label("Proxy:"), proxyHost, Label(":"), proxyPort)
        }
    }

}
