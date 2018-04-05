package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.component.Icons
import javafx.scene.layout.Priority
import javafx.stage.Window

private const val DEFAULT_STYLE_CLASS = "command-result-dialog"

class CmdResultDialog(result: String, window: Window) : Dialog<Unit>(window, I18N["dialog.cmdResult.title"], true) {

    init {
        header = I18N["dialog.cmdResult.header"]
        graphic = Icons.terminal()
        content = vbox {
            addClass(DEFAULT_STYLE_CLASS)
            +textArea {
                vgrow(Priority.ALWAYS)
                isEditable = false
                text = result
            }
        }
        +DialogButton(DialogButton.OK)
    }

}
