package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.decrypt
import hamburg.remme.tinygit.encrypt
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.fileChooser
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.passwordField
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.textInputDialog
import javafx.scene.control.Label
import javafx.stage.Window

class SettingsDialog(repository: LocalRepository, window: Window) : Dialog(window, "Repository Settings") {

    init {
        +DialogButton(DialogButton.OK)
        +DialogButton(DialogButton.CANCEL)

        val location = textField {
            columnSpan(3)
            isEditable = false
            text = repository.path
        }
        val url = textField {
            isEditable = false
            text = Git.getRemote(repository)
        }
        val urlSet = button {
            columnSpan(2)
            fillWidth()
            graphic = FontAwesome.link()
            maxWidth = Double.MAX_VALUE
            setOnAction {
                textInputDialog(window, "Enter Remote URL", FontAwesome.link()) {
                    Git.setRemote(repository, it)
                    url.text = Git.getRemote(repository)
                }
            }
        }
        val ssh = textField {
            text = repository.ssh
        }
        val sshSearch = button {
            columnSpan(2)
            fillWidth()
            graphic = FontAwesome.search()
            maxWidth = Double.MAX_VALUE
            setOnAction { fileChooser(window, "Choose a SSH Key") { ssh.text = it.absolutePath } }
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

            State.fireRefresh()
        }
        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Location:"), location,
                    Label("Remote:"), url, urlSet,
                    Label("SSH Key:"), ssh, sshSearch,
                    Label("User:"), username,
                    Label("Password:"), password,
                    Label("Proxy:"), host, Label(":"), port)
        }
    }

}
