package hamburg.remme.tinygit.gui.builder

import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.StringConverter
import javafx.util.converter.IntegerStringConverter

inline fun textField(block: TextFieldBuilder.() -> Unit): TextField {
    val textField = TextFieldBuilder()
    block.invoke(textField)
    return textField
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
    block.invoke(textArea)
    return textArea
}

inline fun checkBox(block: CheckBox.() -> Unit): CheckBox {
    val checkBox = CheckBox()
    block.invoke(checkBox)
    return checkBox
}

inline fun <T> comboBox(block: ComboBox<T>.() -> Unit): ComboBox<T> {
    val comboBox = ComboBox<T>()
    block.invoke(comboBox)
    return comboBox
}

inline fun <T> comboBox(items: ObservableList<T>, block: ComboBox<T>.() -> Unit): ComboBox<T> {
    val comboBox = ComboBox<T>(items)
    block.invoke(comboBox)
    return comboBox
}

class TextFieldBuilder : TextField() {

    fun intFormatter(value: Int) {
        textFormatter = TextFormatter<Int>(IntegerStringConverter(), value)
    }

    fun emailFormatter(value: String = "") {
        textFormatter = TextFormatter<String>(object : StringConverter<String>() {
            override fun toString(`object`: String?) = `object`.toString()
            override fun fromString(string: String?) = string?.takeIf { it.matches(".+@.+\\..+".toRegex()) } ?: ""
        }, value)
    }

}
