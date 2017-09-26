package hamburg.remme.tinygit.gui

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.util.Callback
import java.time.format.DateTimeFormatter

val shortDate = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val fullDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!

fun button(label: String = "",
           icon: Node? = null,
           action: EventHandler<ActionEvent>,
           tooltip: String? = null,
           disable: BooleanBinding? = null,
           vararg styleClass: String): Button {
    val button = Button(label)
    button.onAction = action
    button.styleClass += styleClass
    icon?.let { button.graphic = it }
    tooltip?.let { button.tooltip = Tooltip(it) }
    disable?.let { button.disableProperty().bind(it) }
    return button
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

fun textArea(value: String = "",
             placeholder: String = "",
             editable: Boolean = true): TextArea {
    val textArea = TextArea(value)
    textArea.promptText = placeholder
    textArea.isEditable = editable
    textArea.isWrapText = true
    return textArea
}

fun spacer() = Pane().also { HBox.setHgrow(it, Priority.ALWAYS) }
