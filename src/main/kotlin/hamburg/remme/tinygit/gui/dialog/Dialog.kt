package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.disabledWhen
import hamburg.remme.tinygit.gui.builder.imageView
import javafx.beans.value.ObservableBooleanValue
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import javafx.scene.control.Dialog as FXDialog

abstract class Dialog(window: Window, title: String, resizable: Boolean = false) {

    protected var content: Node
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.dialogPane.content = value
        }
    protected var header: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.dialogPane.headerText = value
        }
    protected var graphic: Image
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            dialog.graphic = imageView {
                addClass("icon")
                image = value
                isSmooth = true
                fitWidth = 32.0
                fitHeight = 32.0
            }
        }
    protected var okAction: () -> Unit = {}
    protected var cancelAction: () -> Unit = {}
    protected var focusAction: () -> Unit = {}
    private val dialog: FXDialog<Unit> = FXDialog()

    init {
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(window)
        dialog.title = title
        dialog.isResizable = resizable
        dialog.resultConverter = Callback {
            when {
                it == null -> cancelAction.invoke()
                it.buttonData == ButtonBar.ButtonData.OK_DONE -> okAction.invoke()
                it.buttonData == ButtonBar.ButtonData.CANCEL_CLOSE -> cancelAction.invoke()
            }
        }
        dialog.dialogPane.scene.window.focusedProperty().addListener { _, _, it -> if (it) focusAction.invoke() }
    }

    fun show() {
        State.modalVisible.set(true)
        dialog.show()
    }

    protected operator fun DialogButton.unaryPlus() {
        dialog.dialogPane.buttonTypes.add(type)
        disabled?.let { dialog.dialogPane.lookupButton(type).disabledWhen(it) }
    }

    protected class DialogButton(val type: ButtonType, val disabled: ObservableBooleanValue? = null) {

        companion object {
            val OK = ButtonType("OK", ButtonBar.ButtonData.OK_DONE)
            val DONE = ButtonType("Done", ButtonBar.ButtonData.OK_DONE)
            val CANCEL = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        }

    }

}
