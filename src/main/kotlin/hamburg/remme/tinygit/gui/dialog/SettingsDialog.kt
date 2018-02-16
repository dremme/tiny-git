package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.git.gitAddRemote
import hamburg.remme.tinygit.git.gitGetProxy
import hamburg.remme.tinygit.git.gitGetUserEmail
import hamburg.remme.tinygit.git.gitGetUserName
import hamburg.remme.tinygit.git.gitRemoveRemote
import hamburg.remme.tinygit.git.gitSetProxy
import hamburg.remme.tinygit.git.gitSetPushUrl
import hamburg.remme.tinygit.git.gitSetUrl
import hamburg.remme.tinygit.git.gitSetUserEmail
import hamburg.remme.tinygit.git.gitSetUserName
import hamburg.remme.tinygit.git.gitUnsetProxy
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.autocomplete
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.textField
import javafx.scene.control.Label
import javafx.stage.Window

class SettingsDialog(window: Window) : Dialog<Unit>(window, "Repository Settings") {

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
        val authorName = textField {
            prefWidth = 300.0
            text = originalName
            promptText = "Sherlock Holmes"
        }
        val authorEmail = textField {
            prefWidth = 300.0
            promptText = "sherlock.holmes@baker-street.co.uk"
            emailFormatter(originalEmail)
        }
        val proxy = autocomplete(service.usedProxies) {
            fillWidth()
            value = originalProxy
            promptText = "http://proxy.domain:80"
        }

        content = grid(2) {
            addClass("settings-view")
            +listOf(Label("Remote:"), remote,
                    Label("Location:"), location,
                    Label("Author Name:"), authorName,
                    Label("Author Email:"), authorEmail,
                    Label("Proxy:"), proxy)
        }

        +DialogButton(DialogButton.OK)
        +DialogButton(DialogButton.CANCEL)

        okAction = {
            val url = remote.text
            if (url != originalUrl) {
                if (url.isNotBlank()) {
                    if (service.hasRemote.get()) {
                        gitSetUrl(repository, url)
                        gitSetPushUrl(repository, url)
                    } else {
                        gitAddRemote(repository, url)
                    }
                } else {
                    gitRemoveRemote(repository)
                }
            }

            val name = authorName.text
            val email = authorEmail.text
            if (name != originalName) gitSetUserName(repository, name)
            if (email != originalEmail) gitSetUserEmail(repository, email)

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
