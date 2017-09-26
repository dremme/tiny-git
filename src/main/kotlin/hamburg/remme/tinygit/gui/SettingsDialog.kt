package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalCredentials
import hamburg.remme.tinygit.git.LocalRepository
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextFormatter
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import javafx.util.converter.IntegerStringConverter

class SettingsDialog(repository: LocalRepository, window: Window) : Dialog<Unit>() {

    init {
        title = "Repository Settings"
        initModality(Modality.WINDOW_MODAL)
        initOwner(window)

        val ok = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        val url = textField(repository.path, editable = false)
        val ssh = textField(repository.credentials?.ssh ?: "")
        val username = textField(repository.credentials?.username ?: "")
        val password = PasswordField().also { it.text = repository.credentials?.password ?: "" }
        val host = textField(repository.proxyHost ?: "")
        val port = textField().also { it.prefColumnCount = 4 }
        port.textFormatter = TextFormatter<Int>(IntegerStringConverter(), repository.proxyPort)

        val content = GridPane()
        content.styleClass += "settings-view"
        content.add(Label("URL:"), 0, 0)
        content.add(url, 1, 0, 3, 1)
        content.add(Label("SSH Key:"), 0, 1)
        content.add(ssh, 1, 1, 3, 1)
        content.add(Label("User:"), 0, 2)
        content.add(username, 1, 2, 3, 1)
        content.add(Label("Password:"), 0, 3)
        content.add(password, 1, 3, 3, 1)
        content.add(Label("Proxy:"), 0, 4)
        content.add(host, 1, 4)
        content.add(Label(":"), 2, 4)
        content.add(port, 3, 4)

        resultConverter = Callback {
            if (it != null && it.buttonData.isDefaultButton) {
                if (username.text.isNotBlank()) {
                    repository.credentials = LocalCredentials(ssh.text, username.text, password.text)
                }
                repository.proxyHost = host.text
                repository.proxyPort = port.text.toInt()
//                State.fireRefresh()
            }
        }
        dialogPane.content = content
        dialogPane.buttonTypes.addAll(cancel, ok)
    }

}
