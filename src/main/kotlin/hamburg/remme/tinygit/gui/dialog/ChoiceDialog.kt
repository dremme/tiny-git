package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.vbox
import javafx.application.Platform
import javafx.scene.control.ComboBox
import javafx.stage.Window

private const val DEFAULT_STYLE_CLASS = "choice-dialog"

class ChoiceDialog<T>(ok: String, window: Window) : Dialog<T>(window, I18N["dialog.select.title"]) {

    var items: List<T>
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            input.items.setAll(value)
            input.selectionModel.selectFirst()
        }
    var description: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            label.text = value
        }
    private val label = label { managedWhen(textProperty().isNotEmpty) }
    private val input = ComboBox<T>()

    init {
        Platform.runLater { input.requestFocus() }

        content = vbox {
            addClass(DEFAULT_STYLE_CLASS)
            +label
            +input
        }

        +DialogButton(DialogButton.ok(ok))
        +DialogButton(DialogButton.CANCEL)

        okAction = { input.value }
    }

}
