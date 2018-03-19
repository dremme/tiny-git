package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.gui.builder.addStylesheet
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.stage.Window

class TextInputDialog(ok: String, textArea: Boolean, window: Window) : Dialog<String>(window, I18N["dialog.input.title"], textArea) {

    var defaultValue: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            input.text = value
        }
    var description: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            label.text = value
        }
    private val label = Label().apply { managedWhen(textProperty().isNotEmpty) }
    private val input = if (textArea) textArea { vgrow(Priority.ALWAYS) } else TextField()

    init {
        Platform.runLater { input.requestFocus() }

        content = vbox {
            addStylesheet("input-dialog.css")
            spacing = 6.0
            +label
            +input
        }

        +DialogButton(DialogButton.ok(ok))
        +DialogButton(DialogButton.CANCEL)

        okAction = { input.text }
    }

}
