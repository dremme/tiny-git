package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.FontAwesome
import hamburg.remme.tinygit.gui._button
import hamburg.remme.tinygit.gui._intTextField
import hamburg.remme.tinygit.gui._textField
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.fileChooser
import javafx.event.EventHandler
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.layout.GridPane
import javafx.stage.Window

class SettingsDialog(repository: LocalRepository, window: Window) : Dialog(window, "Repository Settings") {

    init {
        val ok = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
        val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        val location = _textField(repository.path, editable = false)
        val url = _textField(Git.url(repository), editable = false)
        val ssh = _textField(repository.ssh)
        val sshSearch = _button(
                icon = FontAwesome.folderOpen(),
                action = EventHandler { fileChooser(window, "Choose a SSH Key")?.let { ssh.text = it.absolutePath } })
                .also {
                    it.maxWidth = Double.MAX_VALUE
                    GridPane.setFillWidth(it, true)
                }
        val username = _textField(repository.username)
        val password = PasswordField().also { it.text = repository.password }
        val host = _textField(repository.proxyHost ?: "")
        val port = _intTextField(repository.proxyPort ?: 80).also { it.prefColumnCount = 4 }

        var row = 0
        val content = GridPane().addClass("settings-view")
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

        okAction = {
            repository.ssh = ssh.text
            repository.username = username.text
            repository.password = password.text
            repository.proxyHost = host.text
            repository.proxyPort = port.text.toInt()

            State.fireRefresh()
        }
        setContent(content)
        setButton(cancel, ok)
    }

}
