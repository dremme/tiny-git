package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.decrypt
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.encrypt
import hamburg.remme.tinygit.git.gitAddRemote
import hamburg.remme.tinygit.git.gitGetUrl
import hamburg.remme.tinygit.git.gitHasRemote
import hamburg.remme.tinygit.git.gitRemoveRemote
import hamburg.remme.tinygit.git.gitSetPushUrl
import hamburg.remme.tinygit.git.gitSetUrl
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.fileChooser
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.passwordField
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.component.Icons
import javafx.scene.control.Label
import javafx.stage.Window

// TODO: should be wider
class SettingsDialog(repository: Repository, window: Window) : Dialog<Unit>(window, "Repository Settings") {

    init {
        +DialogButton(DialogButton.OK)
        +DialogButton(DialogButton.CANCEL)

        val url = textField {
            isEditable = false
            text = gitGetUrl(repository)
        }
        val urlSet = button {
            columnSpan(2)
            fillWidth()
            graphic = Icons.link()
            maxWidth = Double.MAX_VALUE
            setOnAction {
                textInputDialog(dialogWindow, "Enter Remote URL", "Apply", Icons.link(), gitGetUrl(repository)) {
                    // TODO: make removal more clear
                    if (it.isNotBlank()) {
                        if (gitHasRemote(repository)) {
                            gitSetUrl(repository, it)
                            gitSetPushUrl(repository, it)
                        } else gitAddRemote(repository, it)
                    } else gitRemoveRemote(repository)
                    url.text = gitGetUrl(repository)
                }
            }
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
            setOnAction { fileChooser(dialogWindow, "Choose a SSH Key") { ssh.text = it.absolutePath } }
        }
        val username = textField {
            columnSpan(3)
            text = repository.username
        }
        val password = passwordField {
            columnSpan(3)
            text = repository.password.decrypt()
        }
        val host = textField { text = repository.proxyHost }
        val port = textField {
            prefColumnCount = 4
            intFormatter(repository.proxyPort)
        }

        okAction = {
            repository.ssh = ssh.text
            repository.username = username.text
            repository.password = password.text.encrypt()
            repository.proxyHost = host.text
            repository.proxyPort = port.text.toInt()

            State.fireRefresh(this)
        }
        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Remote:"), url, urlSet,
                    Label("Location:"), location,
                    Label("SSH Key:"), ssh, sshSearch,
                    Label("User:"), username,
                    Label("Password:"), password,
                    Label("Proxy:"), host, Label(":"), port)
        }
    }

}
