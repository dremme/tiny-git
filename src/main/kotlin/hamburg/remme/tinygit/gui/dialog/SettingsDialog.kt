package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.git.gitAddRemote
import hamburg.remme.tinygit.git.gitGetUrl
import hamburg.remme.tinygit.git.gitHasRemote
import hamburg.remme.tinygit.git.gitRemoveRemote
import hamburg.remme.tinygit.git.gitSetProxy
import hamburg.remme.tinygit.git.gitSetPushUrl
import hamburg.remme.tinygit.git.gitSetUrl
import hamburg.remme.tinygit.git.gitUnsetProxy
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.textField
import javafx.scene.control.Label
import javafx.stage.Window

// TODO: should be wider
class SettingsDialog(window: Window) : Dialog<Unit>(window, "Repository Settings") {

    private val repository = RepositoryService.activeRepository.get()!!
    private val originalUrl = gitGetUrl(repository)
    private val originalHost = repository.proxyHost
    private val originalPort = repository.proxyPort

    init {
        +DialogButton(DialogButton.OK)
        +DialogButton(DialogButton.CANCEL)

        val remote = textField {
            columnSpan(3)
            isEditable = true
            text = originalUrl
        }
        val location = textField {
            columnSpan(3)
            isEditable = false
            text = repository.path
        }
        val proxyHost = textField { text = originalHost }
        val proxyPort = textField {
            prefColumnCount = 4
            intFormatter(originalPort)
        }

        okAction = {
            val url = remote.text
            if (url != originalUrl) {
                if (url.isNotBlank()) {
                    if (gitHasRemote(repository)) {
                        gitSetUrl(repository, url)
                        gitSetPushUrl(repository, url)
                    } else gitAddRemote(repository, url)
                } else gitRemoveRemote(repository)
            }

            val host = proxyHost.text
            val port = proxyPort.text.toInt()
            if (host != originalHost || port != originalPort) {
                repository.proxyHost = host
                repository.proxyPort = port
                if (host.isNotBlank()) gitSetProxy(repository)
                else gitUnsetProxy(repository)
            }

            State.fireRefresh()
        }
        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Remote:"), remote,
                    Label("Location:"), location,
                    Label("Proxy:"), proxyHost, Label(":"), proxyPort)
        }
    }

}
