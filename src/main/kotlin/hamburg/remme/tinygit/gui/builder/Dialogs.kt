package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.gui.dialog.ChoiceDialog
import hamburg.remme.tinygit.gui.dialog.TextInputDialog
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import java.io.File

fun ButtonType.isOk() = buttonData == ButtonBar.ButtonData.OK_DONE

fun ButtonType.isCancel() = buttonData == ButtonBar.ButtonData.CANCEL_CLOSE

fun confirmAlert(window: Window, header: String, ok: String, text: String): Boolean {
    val alert = alert(window,
            Alert.AlertType.CONFIRMATION,
            header,
            text,
            Icons.questionCircle().addClass("info"),
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
            Icons.exclamationTriangle().addClass("warning"),
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
            Icons.exclamationTriangle().addClass("error"),
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

inline fun textInputDialog(window: Window,
                           header: String,
                           ok: String,
                           icon: Node,
                           defaultValue: String = "",
                           block: (String) -> Unit) {
    val dialog = TextInputDialog(ok, false, window)
    dialog.header = header
    dialog.graphic = icon
    dialog.defaultValue = defaultValue
    dialog.showAndWait()?.let(block)
}

inline fun textAreaDialog(window: Window,
                          header: String,
                          ok: String,
                          icon: Node,
                          defaultValue: String = "",
                          description: String = "",
                          block: (String) -> Unit) {
    val dialog = TextInputDialog(ok, true, window)
    dialog.header = header
    dialog.graphic = icon
    dialog.defaultValue = defaultValue
    dialog.description = description
    dialog.showAndWait()?.let(block)
}

inline fun choiceDialog(window: Window,
                        header: String,
                        ok: String,
                        icon: Node,
                        items: List<String>,
                        description: String = "",
                        block: (String) -> Unit) {
    val dialog = ChoiceDialog(ok, window)
    dialog.header = header
    dialog.graphic = icon
    dialog.items = items
    dialog.description = description
    dialog.showAndWait()?.let(block)
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
