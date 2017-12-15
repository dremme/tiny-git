package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.encrypt
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.directoryChooser
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.fileChooser
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.passwordField
import hamburg.remme.tinygit.gui.builder.textField
import javafx.scene.control.Label
import javafx.stage.Window

// TODO: should be wider
class CloneDialog(window: Window) : Dialog<Unit>(window, "Clone Repository") {

    init {
        val location = textField { }
        val locationSet = button {
            columnSpan(2)
            fillWidth()
            graphic = FontAwesome.search()
            maxWidth = Double.MAX_VALUE
            setOnAction { directoryChooser(dialogWindow, "Choose a Directory") { location.text = it.absolutePath } }
        }
        val url = textField { columnSpan(3) }
        val ssh = textField { }
        val sshSearch = button {
            columnSpan(2)
            fillWidth()
            graphic = FontAwesome.search()
            maxWidth = Double.MAX_VALUE
            setOnAction { fileChooser(dialogWindow, "Choose a SSH Key") { ssh.text = it.absolutePath } }
        }
        val username = textField { columnSpan(3) }
        val password = passwordField { columnSpan(3) }
        val host = textField { }
        val port = textField {
            prefColumnCount = 4
            intFormatter(80)
        }

        +DialogButton(DialogButton.ok("Clone"),
                location.textProperty().isEmpty.or(url.textProperty().isEmpty))
        +DialogButton(DialogButton.CANCEL)

        okAction = {
            val repository = LocalRepository(location.text)
            repository.ssh = ssh.text
            repository.username = username.text
            repository.password = password.text.encrypt()
            repository.proxyHost = host.text
            repository.proxyPort = port.text.toInt()

            try {
                Git.clone(url.text, location.text)
            } catch (ex: Exception) {
                errorAlert(dialogWindow, "Cannot Clone Repository", ex.message!!)
                throw ex
            }

            State.addRepository(repository)
        }
        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Location:"), location, locationSet,
                    Label("Remote:"), url,
                    Label("SSH Key:"), ssh, sshSearch,
                    Label("User:"), username,
                    Label("Password:"), password,
                    Label("Proxy:"), host, Label(":"), port)
        }
    }

}
