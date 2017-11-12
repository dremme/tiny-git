package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCredentials
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.gui.FontAwesome.folderOpen
import hamburg.remme.tinygit.gui.button
import hamburg.remme.tinygit.gui.intTextField
import hamburg.remme.tinygit.gui.textField
import javafx.event.EventHandler
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.layout.GridPane
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import java.io.File

class SettingsDialog(repository: LocalRepository, window: Window) : Dialog<Unit>() {

    init {
        title = "Repository Settings"
        initModality(Modality.WINDOW_MODAL)
        initOwner(window)

        val ok = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        val location = textField(repository.path, editable = false)
        val url = textField(LocalGit.url(repository), editable = false)
        val ssh = textField(repository.credentials?.ssh ?: "")
        val sshSearch = button(
                icon = folderOpen(),
                action = EventHandler {
                    val chooser = FileChooser()
                    chooser.title = "Choose a SSH Key"
                    chooser.initialDirectory = File(System.getProperty("user.home"))
                    chooser.showOpenDialog(this.owner)?.let { ssh.text = it.absolutePath }
                })
                .also {
                    it.maxWidth = Double.MAX_VALUE
                    GridPane.setFillWidth(it, true)
                }
        val username = textField(repository.credentials?.username ?: "")
        val password = PasswordField().also { it.text = repository.credentials?.password ?: "" }
        val host = textField(repository.proxyHost ?: "")
        val port = intTextField(repository.proxyPort ?: 80).also { it.prefColumnCount = 4 }

        var row = 0
        val content = GridPane()
        content.styleClass += "settings-view"
        content.add(Label("Location:"), 0, row)
        content.add(location, 1, row++, 3, 1)
        content.add(Label("Remote:"), 0, row)
        content.add(url, 1, row++, 3, 1)
        content.add(Label("SSH Key:"), 0, row)
        content.add(ssh, 1, row, 2, 1)
        content.add(sshSearch, 3, row++)
        content.add(Label("User:"), 0, row)
        content.add(username, 1, row++, 3, 1)
        content.add(Label("Password:"), 0, row)
        content.add(password, 1, row++, 3, 1)
        content.add(Label("Proxy:"), 0, row)
        content.add(host, 1, row)
        content.add(Label(":"), 2, row)
        content.add(port, 3, row)

        resultConverter = Callback {
            if (it.buttonData.isDefaultButton) {
                if (ssh.text.isNotBlank() || username.text.isNotBlank()) {
                    repository.credentials = LocalCredentials(ssh.text, username.text, password.text)
                } else {
                    repository.credentials = null
                }
                repository.proxyHost = host.text
                repository.proxyPort = port.text.toInt()

                State.fireRefresh()
            }
        }
        dialogPane.content = content
        dialogPane.buttonTypes.addAll(cancel, ok)
    }

}
