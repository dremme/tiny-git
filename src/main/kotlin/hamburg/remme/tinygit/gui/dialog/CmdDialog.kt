package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStylesheet
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.component.Icons
import javafx.application.Platform
import javafx.stage.Window

class CmdDialog(repository: Repository, window: Window) : Dialog<Array<String>>(window, "Command") {

    init {
        val input = textField { Platform.runLater { requestFocus() } }

        header = "Execute Git Command"
        graphic = Icons.terminal()
        content = vbox {
            addStylesheet("input-dialog.css")
            spacing = 6.0
            +label { +"Execute any Git command in '${repository.path}':" }
            +hbox {
                addClass("git")
                +label { +"git" }
                +input
            }
        }

        +DialogButton(DialogButton.ok("Execute"))
        +DialogButton(DialogButton.CANCEL)

        okAction = { input.text.split(" +".toRegex()).toTypedArray() }
    }

}
