package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.directoryChooser
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.fillWidth
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.component.Icons
import javafx.scene.control.Label
import javafx.stage.Window

// TODO: should be wider
class CloneDialog(window: Window) : Dialog<Unit>(window, "Clone Repository") {

    private val repoService = TinyGit.repositoryService

    init {
        val url = textField { columnSpan(3) }
        val location = textField { }
        val locationSet = button {
            columnSpan(2)
            fillWidth()
            graphic = Icons.search()
            maxWidth = Double.MAX_VALUE
            setOnAction { directoryChooser(dialogWindow, "Choose a Directory") { location.text = it.toString() } }
        }
        val host = textField { }
        val port = textField {
            prefColumnCount = 4
            intFormatter(80)
        }

        +DialogButton(DialogButton.ok("Clone"),
                location.textProperty().isEmpty.or(url.textProperty().isEmpty))
        +DialogButton(DialogButton.CANCEL)

        okAction = {
            val repository = Repository(location.text)
            repository.proxyHost = host.text
            repository.proxyPort = port.text.toInt()
            repoService.clone(repository, url.text, { errorAlert(window, "Cannot Clone Repository", it) })
        }
        content = grid(4) {
            addClass("settings-view")
            +listOf(Label("Remote:"), url,
                    Label("Location:"), location, locationSet,
                    Label("Proxy:"), host, Label(":"), port)
        }
    }

}
