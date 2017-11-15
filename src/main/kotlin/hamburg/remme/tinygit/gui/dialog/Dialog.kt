package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import javafx.beans.value.ObservableBooleanValue
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import javafx.scene.control.Dialog as FXDialog

abstract class Dialog(window: Window, title: String, resizable: Boolean = false) {

    protected var okAction: () -> Unit = {}
    protected var cancelAction: () -> Unit = {}
    private val dialog: FXDialog<Unit> = FXDialog()

    init {
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(window)
        dialog.title = title
        dialog.isResizable = resizable
        dialog.resultConverter = Callback {
            when (it.buttonData) {
                ButtonBar.ButtonData.OK_DONE -> okAction.invoke()
                ButtonBar.ButtonData.CANCEL_CLOSE -> cancelAction.invoke()
                else -> {
                    // do nothing
                }
            }
        }
    }

    fun show() {
        State.modalVisible.set(true)
        dialog.show()
    }

    fun showAndWait() {
        State.modalVisible.set(true)
        dialog.showAndWait()
    }

    protected fun setContent(content: Node) {
        dialog.dialogPane.content = content
    }

    protected fun setButton(vararg button: ButtonType) {
        dialog.dialogPane.buttonTypes.setAll(*button)
    }

    protected fun setButtonBinding(button: ButtonType, disable: ObservableBooleanValue) {
        dialog.dialogPane.lookupButton(button).disableProperty().bind(disable)
    }

}
