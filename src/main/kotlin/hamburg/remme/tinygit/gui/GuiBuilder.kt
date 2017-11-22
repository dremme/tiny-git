package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.Separator
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ToolBar
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import javafx.util.converter.IntegerStringConverter
import java.io.File
import java.time.format.DateTimeFormatter

fun String.htmlEncode() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

fun String.htmlEncodeSpaces() = replace(" ", "&nbsp;")

fun String.htmlEncodeAll() = htmlEncode().htmlEncodeSpaces()

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                                               *
 * DATES                                                                                                         *
 *                                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
val shortDate = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val fullDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                                               *
 * NODE UTIL                                                                                                     *
 *                                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
fun <T : Node> T.addClass(vararg styleClass: String): T {
    this.styleClass += styleClass
    return this
}

fun <T : Node> T.addStyle(style: String): T {
    this.style += style
    return this
}

fun <T : Node> T.flipX(): T {
    scaleX = -1.0
    return this
}

fun <T : Node> T.flipY(): T {
    scaleY = -1.0
    return this
}

fun <T : Node> T.flipXY() = flipX().flipY()

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                                               *
 * UI BUILDER                                                                                                    *
 *                                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
/*
 * TODO: create a proper, declarative, ui-builder
 * TODO: fun label(block: Label.() -> Unit) = Label().also { block.invoke(it) }
 */
fun label(text: String = "",
          icon: Node? = null,
          color: String? = null,
          tooltip: String? = null): Label {
    val label = Label(text)
    icon?.let { label.graphic = it }
    color?.let { label.style += "-fx-text-fill:$it;" }
    tooltip?.let { label.tooltip = Tooltip(it) }
    return label
}

fun button(label: String = "",
           icon: Node? = null,
           action: EventHandler<ActionEvent>,
           tooltip: String? = null,
           disable: ObservableBooleanValue? = null,
           vararg styleClass: String): Button {
    val button = Button(label)
    button.onAction = action
    button.styleClass += styleClass
    icon?.let { button.graphic = it }
    tooltip?.takeIf { it.isNotBlank() }?.let { button.tooltip = Tooltip(it) }
    disable?.let { button.disableProperty().bind(it) }
    return button
}

fun button(action: Action): Button {
    return button(action.text, action.icon.invoke(), action.action, action.shortcut?.keyCombinationText(), action.disable)
}

fun <S, T> tableColumn(title: String,
                       sortable: Boolean = false,
                       cellValue: Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>>,
                       cellFactory: Callback<TableColumn<S, T>, TableCell<S, T>>? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = cellValue
    column.isSortable = sortable
    cellFactory?.let { column.cellFactory = it }
    return column
}

fun textField(value: String = "",
              editable: Boolean = true): TextField {
    val textField = TextField(value)
    textField.isEditable = editable
    return textField
}

fun intTextField(value: Int = 0,
                 editable: Boolean = true): TextField {
    val textField = textField(value.toString(), editable)
    textField.textFormatter = TextFormatter<Int>(IntegerStringConverter(), value)
    return textField
}

fun textArea(value: String = "",
             placeholder: String = "",
             editable: Boolean = true): TextArea {
    val textArea = TextArea(value)
    textArea.promptText = placeholder
    textArea.isEditable = editable
    textArea.isWrapText = true
    return textArea
}

fun spacer(): Node = Pane().also { HBox.setHgrow(it, Priority.ALWAYS) }

fun menuItem(label: String,
             icon: Node? = null,
             shortcut: String? = null,
             action: EventHandler<ActionEvent>,
             disable: ObservableBooleanValue? = null): MenuItem {
    val menuItem = MenuItem(label)
    menuItem.onAction = action
    shortcut?.let { menuItem.accelerator = KeyCombination.valueOf(it) }
    icon?.let { menuItem.graphic = it }
    disable?.let { menuItem.disableProperty().bind(it) }
    return menuItem
}

fun menuBar(vararg collection: ActionCollection): MenuBar {
    val menuBar = MenuBar(*collection.map { col ->
        Menu(col.text, null, *col.group.mapIndexed { i, it ->
            it.action.map { menuItem(it.text, it.icon.invoke(), it.shortcut, it.action, it.disable) }
                    .let { if (i < col.group.size - 1) it + SeparatorMenuItem() else it }
        }.flatten().toTypedArray())
    }.toTypedArray())
    menuBar.isUseSystemMenuBar = true
    return menuBar
}

fun toolBar(vararg group: ActionGroup): ToolBar {
    return ToolBar(*group.mapIndexed { i, it ->
        it.action.map {
            if (it.count != null) {
                val count = Label().addClass("count-badge")
                count.textProperty().bind(toolBarBadge(it.count))
                count.visibleProperty().bind(Bindings.notEqual(0, it.count))
                StackPane.setAlignment(count, Pos.TOP_RIGHT)
                StackPane(button(it), count)
            } else {
                button(it)
            }
        }.let {
            if (i < group.size - 1) it + Separator() else it
        }
    }.flatten().toTypedArray())
}

private fun toolBarBadge(count: ObservableIntegerValue): StringBinding {
    return Bindings.createStringBinding({ if (count.get() > 0) count.get().toString() else "*" }, arrayOf(count))!!
}

private fun String.keyCombinationText() = KeyCombination.valueOf(this).displayText

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                                               *
 * DIALOGS                                                                                                       *
 *                                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
fun confirmAlert(window: Window,
                 header: String,
                 text: String): Boolean {
    val alert = alert(window, Alert.AlertType.CONFIRMATION, header, text, FontAwesome.questionCircle("#5bc0de"))
    State.modalVisible.set(true)
    return alert.showAndWait().get() == ButtonType.OK
}

fun confirmWarningAlert(window: Window,
                        header: String,
                        text: String): Boolean {
    val alert = alert(window, Alert.AlertType.CONFIRMATION, header, text, FontAwesome.exclamationTriangle("#f0ad4e"))
    State.modalVisible.set(true)
    return alert.showAndWait().get() == ButtonType.OK
}

fun errorAlert(window: Window,
               header: String,
               text: String) {
    val alert = alert(window, Alert.AlertType.ERROR, header, text, FontAwesome.exclamationTriangle("#d9534f"))
    State.modalVisible.set(true)
    alert.showAndWait()
}

private fun alert(window: Window,
                  type: Alert.AlertType,
                  header: String,
                  text: String,
                  icon: Node): Alert {
    val alert = Alert(type, text)
    alert.initModality(Modality.WINDOW_MODAL)
    alert.initOwner(window)
    alert.headerText = header
    alert.graphic = icon
    return alert
}

fun textInputDialog(window: Window,
                    icon: Node? = null): String? {
    val dialog = TextInputDialog()
    dialog.initModality(Modality.WINDOW_MODAL)
    dialog.initOwner(window)
    dialog.title = "Input"
    dialog.headerText = "Enter a New Branch Name"
    dialog.graphic = icon
    State.modalVisible.set(true)
    return dialog.showAndWait().orElse(null)
}

fun fileChooser(window: Window, title: String): File? {
    val chooser = FileChooser()
    chooser.title = title
    chooser.initialDirectory = File(System.getProperty("user.home"))
    State.modalVisible.set(true)
    return chooser.showOpenDialog(window)
}

fun directoryChooser(window: Window, title: String): File? {
    val chooser = DirectoryChooser()
    chooser.title = title
    State.modalVisible.set(true)
    return chooser.showDialog(window)
}
