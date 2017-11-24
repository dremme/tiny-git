package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.passwordField
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.fileChooser
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.stage.Window

class SettingsDialog(repository: LocalRepository, window: Window) : Dialog(window, "Repository Settings") {

    init {
        val ok = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        val location = textField {
            columnSpan(3)
            isEditable = false
            text = repository.path
        }
        val url = textField {
            columnSpan(3)
            isEditable = false
            text = Git.url(repository)
        }
        val ssh = textField {
            text = repository.ssh
        }
        val sshSearch = button {
            columnSpan(2)
            fillWidth()
            graphic = FontAwesome.folderOpen()
            maxWidth = Double.MAX_VALUE
            setOnAction { fileChooser(window, "Choose a SSH Key") { ssh.text = it.absolutePath } }
        }
        val username = textField {
            columnSpan(3)
            text = repository.username
        }
        val password = passwordField {
            columnSpan(3)
            text = repository.password
        }
        val host = textField { text = repository.proxyHost }
        val port = textField {
            prefColumnCount = 4
            intFormatter(repository.proxyPort)
        }

        okAction = {
            repository.ssh = ssh.text
            repository.username = username.text
            repository.password = password.text
            repository.proxyHost = host.text
            repository.proxyPort = port.text.toInt()

            State.fireRefresh()
        }
        setButton(cancel, ok)
        setContent(grid {
            addClass("settings-view")
            addRow(Label("Location:"), location)
            addRow(Label("Remote:"), url)
            addRow(Label("SSH Key:"), ssh, sshSearch)
            addRow(Label("User:"), username)
            addRow(Label("Password:"), password)
            addRow(Label("Proxy:"), host, Label(":"), port)
        })
    }

}
