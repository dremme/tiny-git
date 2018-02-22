package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitSetUserEmail
import hamburg.remme.tinygit.git.gitSetUserName
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.autocomplete
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.directoryChooser
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.component.Icons
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.stage.Window

class CloneDialog(window: Window) : Dialog<Unit>(window, "Clone Repository") {

    private val service = TinyGit.repositoryService

    init {
        val url = textField {
            columnSpan(2)
            prefWidth = 300.0
            promptText = "https://github.com/..."
            Platform.runLater { requestFocus() }
        }
        val location = textField {
            prefWidth = 300.0
            promptText = "/home/sherlock/projects/..."
        }
        val locationSet = button {
            fillWidth()
            graphic = Icons.search()
            maxWidth = Double.MAX_VALUE
            setOnAction { directoryChooser(dialogWindow, "Choose a Directory") { location.text = it.toString() } }
        }
        val authorName = textField {
            columnSpan(2)
            prefWidth = 300.0
            promptText = "Sherlock Holmes"
        }
        val authorEmail = textField {
            columnSpan(2)
            prefWidth = 300.0
            promptText = "sherlock.holmes@baker-street.co.uk"
            emailFormatter()
        }
        val proxy = autocomplete(service.usedProxies) {
            columnSpan(2)
            fillWidth()
            promptText = "http://proxy.domain:80"
        }

        content = grid(3) {
            addClass("settings-view")
            +listOf(Label("Remote:"), url,
                    Label("Location:"), location, locationSet,
                    Label("Author Name:"), authorName,
                    Label("Author Email:"), authorEmail,
                    Label("Proxy:"), proxy)
        }

        +DialogButton(DialogButton.ok("Clone"), location.textProperty().isEmpty.or(url.textProperty().isEmpty))
        +DialogButton(DialogButton.CANCEL)

        okAction = {
            val repository = Repository(location.text)
            val name = authorName.text
            val email = authorEmail.text
            val hostPort = proxy.value ?: ""
            service.clone(repository, url.text, hostPort,
                    {
                        if (name.isNotBlank()) gitSetUserName(repository, name)
                        if (email.isNotBlank()) gitSetUserEmail(repository, email)
                        if (hostPort.isNotBlank()) service.addUsedProxy(hostPort)
                    },
                    { errorAlert(window, "Cannot Clone Repository", it) })
        }
    }

}
