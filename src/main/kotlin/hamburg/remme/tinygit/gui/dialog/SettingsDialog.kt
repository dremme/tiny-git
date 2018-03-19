package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.git.gitGetProxy
import hamburg.remme.tinygit.git.gitGetUserEmail
import hamburg.remme.tinygit.git.gitGetUserName
import hamburg.remme.tinygit.git.gitSetProxy
import hamburg.remme.tinygit.git.gitSetUserEmail
import hamburg.remme.tinygit.git.gitSetUserName
import hamburg.remme.tinygit.git.gitUnsetProxy
import hamburg.remme.tinygit.git.gitUnsetUserEmail
import hamburg.remme.tinygit.git.gitUnsetUserName
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.autocomplete
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.textField
import javafx.scene.control.Label
import javafx.stage.Window

class SettingsDialog(window: Window) : Dialog<Unit>(window, I18N["dialog.settings.title"]) {

    private val service = TinyGit.repositoryService
    private val repository = service.activeRepository.get()!!
    private val originalUrl = service.remote.get()!!
    private val originalName = gitGetUserName(repository)
    private val originalEmail = gitGetUserEmail(repository)
    private val originalProxy = gitGetProxy(repository)

    init {
        val remote = textField {
            prefWidth = 300.0
            text = originalUrl
            promptText = "https://github.com/..."
        }
        val location = textField {
            prefWidth = 300.0
            isEditable = false
            text = repository.path
            promptText = "/home/sherlock/projects/..."
        }
        val userName = autocomplete(service.usedNames) {
            fillWidth()
            promptText = "Sherlock Holmes"
            value = originalName
        }
        val userEmail = autocomplete(service.usedEmails) {
            fillWidth()
            promptText = "sherlock.holmes@baker-street.co.uk"
            value = originalEmail
        }
        val proxy = autocomplete(service.usedProxies) {
            fillWidth()
            promptText = "http://proxy.domain:80"
            value = originalProxy
        }

        content = grid(2) {
            addClass("settings-view")
            +listOf(Label("${I18N["dialog.settings.remote"]}:"), remote,
                    Label("${I18N["dialog.settings.location"]}:"), location,
                    Label("${I18N["dialog.settings.userName"]}:"), userName,
                    Label("${I18N["dialog.settings.userEmail"]}:"), userEmail,
                    Label("${I18N["dialog.settings.proxy"]}:"), proxy)
        }

        +DialogButton(DialogButton.OK)
        +DialogButton(DialogButton.CANCEL)

        okAction = {
            val url = remote.text
            if (url != originalUrl) {
                if (url.isNotBlank()) service.addOrSetRemote(url)
                else service.removeRemote()
            }

            val name = userName.value
            if (name != originalName) if (name.isNotBlank()) {
                gitSetUserName(repository, name)
                service.addUsedName(name)
            } else {
                gitUnsetUserName(repository)
            }

            val email = userEmail.value
            if (email != originalEmail) if (email.isNotBlank()) {
                gitSetUserEmail(repository, email)
                service.addUsedEmail(email)
            } else {
                gitUnsetUserEmail(repository)
            }

            val hostPort = proxy.value
            if (hostPort != originalProxy) {
                if (hostPort.isNotBlank()) {
                    gitSetProxy(repository, hostPort)
                    service.addUsedProxy(hostPort)
                } else {
                    gitUnsetProxy(repository)
                }
            }

            TinyGit.fireEvent()
        }
    }

}
