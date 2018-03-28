package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.hgrow
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.component.Icons
import javafx.application.Platform
import javafx.scene.layout.Priority
import javafx.stage.Window

private const val DEFAULT_STYLE_CLASS = "command-dialog"
private const val CONTENT_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__content"

class CmdDialog(repository: Repository, window: Window) : Dialog<Array<String>>(window, I18N["dialog.cmd.title"]) {

    init {
        val input = textField {
            hgrow(Priority.ALWAYS)
            Platform.runLater { requestFocus() }
        }

        header = I18N["dialog.cmd.header"]
        graphic = Icons.terminal()
        content = vbox {
            addClass(DEFAULT_STYLE_CLASS)
            +label { +"${I18N["dialog.cmd.text", repository.path]}:" }
            +hbox {
                addClass(CONTENT_STYLE_CLASS)
                +label { +"git" }
                +input
            }
        }

        +DialogButton(DialogButton.ok(I18N["dialog.cmd.button"]))
        +DialogButton(DialogButton.CANCEL)

        okAction = { input.text.split(" +".toRegex()).toTypedArray() }
    }

}
