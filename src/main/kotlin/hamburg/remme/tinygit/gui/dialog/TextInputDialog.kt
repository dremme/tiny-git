package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.gui.builder.isOk
import hamburg.remme.tinygit.gui.builder.vbox
import javafx.application.Platform
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Dialog as FXDialog

// TODO: maybe inherit from our dialog?
class TextInputDialog(ok: String, contextText: String, defaultValue: String, textArea: Boolean) : FXDialog<String>() {

    init {
        val input = if (textArea) TextArea(defaultValue).also { it.prefHeight = 100.0 } else TextField(defaultValue)
        input.minWidth = 300.0
        Platform.runLater { input.requestFocus() }

        dialogPane.content = vbox {
            spacing = 6.0
            if (contextText.isNotBlank()) +Label(contextText)
            +input
        }
        dialogPane.buttonTypes.addAll(ButtonType(ok, ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL)

        setResultConverter { if (it.isOk()) input.text else null }
    }

}
