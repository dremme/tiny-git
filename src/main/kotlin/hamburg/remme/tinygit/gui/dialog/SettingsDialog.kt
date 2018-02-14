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
import hamburg.remme.tinygit.gui.builder.columnSpan
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
    private val originalHost: String
    private val originalPort: Int

    init {
        val originalProxy = gitGetProxy(repository)
        if (originalProxy.isNotEmpty()) {
            originalHost = originalProxy.substringBeforeLast(':')
            originalPort = originalProxy.substringAfterLast(':').toInt()
        } else {
            originalHost = ""
            originalPort = 80
        }

        val remote = textField {
            columnSpan(3)
            prefWidth = 300.0
            text = originalUrl
            promptText = "https://github.com/..."
        }
        val location = textField {
            columnSpan(3)
            prefWidth = 300.0
            isEditable = false
            text = repository.path
            promptText = "/home/sherlock/projects/..."
        }
        val authorName = textField {
            columnSpan(3)
            prefWidth = 300.0
            text = originalName
            promptText = "Sherlock Holmes"
        }
        val authorEmail = textField {
            columnSpan(3)
            prefWidth = 300.0
            promptText = "sherlock.holmes@baker-street.co.uk"
            emailFormatter(originalEmail)
        }
        val proxyHost = textField {
            prefWidth = 300.0
            text = originalHost
            promptText = "http://proxy.domain"
        }
        val proxyPort = textField {
            prefColumnCount = 4
            intFormatter(originalPort)
        }

        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Remote:"), remote,
                    Label("Location:"), location,
                    Label("Author Name:"), authorName,
                    Label("Author Email:"), authorEmail,
                    Label("Proxy:"), proxyHost, Label(":"), proxyPort)
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
                    } else gitAddRemote(repository, url)
                } else gitRemoveRemote(repository)
            }

            val name = authorName.text
            val email = authorEmail.text
            if (name != originalName) gitSetUserName(repository, name)
            if (email != originalEmail) gitSetUserEmail(repository, email)

            val host = proxyHost.text
            val port = proxyPort.text.toInt()
            if (host != originalHost || port != originalPort) {
                if (host.isNotBlank()) gitSetProxy(repository, host, port)
                else gitUnsetProxy(repository)
            }

            TinyGit.fireEvent()
        }
    }

}
