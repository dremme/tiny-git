package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.gui.builder.FontAwesome
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.TextInputDialog
import javafx.scene.input.KeyCombination
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import java.io.File
import java.time.format.DateTimeFormatter

val shortDate = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val fullDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!

fun printError(message: String) {
    System.err.println(message)
}

fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

fun String.htmlEncodeAll() = htmlEncode().htmlEncodeSpaces()

fun String.keyCombinationText() = KeyCombination.valueOf(this).displayText!!

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                                               *
 * DIALOGS                                                                                                       *
 *                                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
fun ButtonType.isOk() = buttonData == ButtonBar.ButtonData.OK_DONE

fun ButtonType.isCancel() = buttonData == ButtonBar.ButtonData.CANCEL_CLOSE

fun confirmAlert(window: Window, header: String, ok: String, text: String): Boolean {
    val alert = alert(window,
            Alert.AlertType.CONFIRMATION,
            header,
            text,
            FontAwesome.questionCircle("#5bc0de"),
            ButtonType(ok, ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL)
    State.modalVisible.set(true)
    return alert.showAndWait().get().isOk()
}

fun confirmWarningAlert(window: Window, header: String, ok: String, text: String): Boolean {
    val alert = alert(window,
            Alert.AlertType.CONFIRMATION,
            header,
            text,
            FontAwesome.exclamationTriangle("#f0ad4e"),
            ButtonType(ok, ButtonBar.ButtonData.OK_DONE),
            ButtonType.CANCEL)
    State.modalVisible.set(true)
    return alert.showAndWait().get().isOk()
}

fun errorAlert(window: Window, header: String, text: String) {
    val alert = alert(window,
            Alert.AlertType.ERROR,
            header,
            text,
            FontAwesome.exclamationTriangle("#d9534f"),
            ButtonType.OK)
    State.modalVisible.set(true)
    alert.showAndWait()
}

private fun alert(window: Window,
                  type: Alert.AlertType,
                  header: String,
                  text: String,
                  icon: Node,
                  vararg button: ButtonType): Alert {
    val alert = Alert(type, text, *button)
    alert.initModality(Modality.WINDOW_MODAL)
    alert.initOwner(window)
    alert.headerText = header
    alert.graphic = icon
    return alert
}

inline fun textInputDialog(window: Window, header: String, icon: Node, block: (String) -> Unit) {
    val dialog = TextInputDialog()
    dialog.initModality(Modality.WINDOW_MODAL)
    dialog.initOwner(window)
    dialog.title = "Input"
    dialog.headerText = header
    dialog.graphic = icon
    State.modalVisible.set(true)
    dialog.showAndWait().orElse(null)?.let(block)
}

inline fun fileChooser(window: Window, title: String, block: (File) -> Unit) {
    val chooser = FileChooser()
    chooser.title = title
    chooser.initialDirectory = File(System.getProperty("user.home"))
    State.modalVisible.set(true)
    chooser.showOpenDialog(window)?.let(block)
}

inline fun directoryChooser(window: Window, title: String, block: (File) -> Unit) {
    val chooser = DirectoryChooser()
    chooser.title = title
    chooser.initialDirectory = File(System.getProperty("user.home"))
    State.modalVisible.set(true)
    chooser.showDialog(window)?.let(block)
}
