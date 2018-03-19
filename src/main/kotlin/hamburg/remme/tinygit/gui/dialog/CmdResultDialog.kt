package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStylesheet
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.component.Icons
import javafx.scene.layout.Priority
import javafx.stage.Window

class CmdResultDialog(result: String, window: Window) : Dialog<Unit>(window, I18N["dialog.cmdResult.title"], true) {

    init {
        header = I18N["dialog.cmdResult.header"]
        graphic = Icons.terminal()
        content = vbox {
            addStylesheet("input-dialog.css")
            addClass("git")
            +textArea {
                vgrow(Priority.ALWAYS)
                isEditable = false
                text = result
            }
        }
        +DialogButton(DialogButton.OK)
    }

}
