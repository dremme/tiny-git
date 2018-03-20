package hamburg.remme.tinygit.gui.builder

import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.PasswordField
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

inline fun textField(block: TextField.() -> Unit): TextField {
    val textField = TextField()
    block(textField)
    return textField
}

inline fun password(block: PasswordField.() -> Unit): PasswordField {
    val password = PasswordField()
    block(password)
    return password
}

inline fun textArea(block: TextArea.() -> Unit): TextArea {
    val textArea = TextArea()
    textArea.addEventFilter(KeyEvent.KEY_PRESSED, {
        if (it.code == KeyCode.TAB && !it.isShiftDown && !it.isControlDown) {
            it.consume()
            (it.source as Node).fireEvent(KeyEvent(it.source, it.target, it.eventType, it.character, it.text, it.code,
                    it.isShiftDown, true, it.isAltDown, it.isMetaDown))
        }
    })
    block(textArea)
    return textArea
}

inline fun checkBox(block: CheckBox.() -> Unit): CheckBox {
    val checkBox = CheckBox()
    block(checkBox)
    return checkBox
}

inline fun <T> comboBox(block: ComboBox<T>.() -> Unit): ComboBox<T> {
    val comboBox = ComboBox<T>()
    block(comboBox)
    return comboBox
}

inline fun <T> comboBox(items: ObservableList<T>, block: ComboBox<T>.() -> Unit): ComboBox<T> {
    val comboBox = ComboBox<T>(items)
    block(comboBox)
    return comboBox
}

inline fun autocomplete(items: ObservableList<String>, block: ComboBox<String>.() -> Unit): ComboBox<String> {
    val comboBox = ComboBox<String>(items)
    comboBox.isEditable = true
    comboBox.focusedProperty().addListener { _, _, it -> if (it && comboBox.items.isNotEmpty()) comboBox.show() }
    block(comboBox)
    return comboBox
}
