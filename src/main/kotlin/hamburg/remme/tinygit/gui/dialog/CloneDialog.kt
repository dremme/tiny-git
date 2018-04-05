package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
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

private const val DEFAULT_STYLE_CLASS = "clone-dialog"

class CloneDialog(window: Window) : Dialog<Unit>(window, I18N["dialog.clone.title"]) {

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
            setOnAction { directoryChooser(dialogWindow, I18N["dialog.chooseCloneDir.title"]) { location.text = it.toString() } }
        }
        val userName = autocomplete(service.usedNames) {
            columnSpan(2)
            fillWidth()
            promptText = "Sherlock Holmes"
        }
        val userEmail = autocomplete(service.usedEmails) {
            columnSpan(2)
            fillWidth()
            promptText = "sherlock.holmes@baker-street.co.uk"
        }
        val proxy = autocomplete(service.usedProxies) {
            columnSpan(2)
            fillWidth()
            promptText = "http://proxy.domain:80"
        }

        content = grid(3) {
            addClass(DEFAULT_STYLE_CLASS)
            +listOf(Label("${I18N["dialog.clone.remote"]}:"), url,
                    Label("${I18N["dialog.clone.location"]}:"), location, locationSet,
                    Label("${I18N["dialog.clone.userName"]}:"), userName,
                    Label("${I18N["dialog.clone.userEmail"]}:"), userEmail,
                    Label("${I18N["dialog.clone.proxy"]}:"), proxy)
        }

        +DialogButton(DialogButton.ok(I18N["dialog.clone.button"]), location.textProperty().isEmpty.or(url.textProperty().isEmpty))
        +DialogButton(DialogButton.CANCEL)

        okAction = {
            val repository = Repository(location.text)
            val name = userName.value ?: ""
            val email = userEmail.value ?: ""
            val hostPort = proxy.value ?: ""
            service.clone(repository, url.text, hostPort,
                    {
                        if (name.isNotBlank()) {
                            gitSetUserName(repository, name)
                            service.addUsedName(name)
                        }
                        if (email.isNotBlank()) {
                            gitSetUserEmail(repository, email)
                            service.addUsedEmail(email)
                        }
                        if (hostPort.isNotBlank()) {
                            service.addUsedProxy(hostPort)
                        }
                    },
                    { errorAlert(window, I18N["dialog.cannotClone.header"], it) })
        }
    }

}
