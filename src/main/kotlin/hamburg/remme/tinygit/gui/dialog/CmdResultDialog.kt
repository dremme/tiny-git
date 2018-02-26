package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStylesheet
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.component.Icons
import javafx.stage.Window

class CmdResultDialog(result: String, window: Window) : Dialog<Unit>(window, "Result") {

    init {
        header = "Git Command Result"
        graphic = Icons.terminal()
        content = vbox {
            addStylesheet("input-dialog.css")
            addClass("git")
            +textArea {
                isEditable = false
                text = result
            }
        }
        +DialogButton(DialogButton.OK)
    }

}
