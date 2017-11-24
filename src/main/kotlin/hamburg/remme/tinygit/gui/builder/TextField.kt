package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.util.converter.IntegerStringConverter

inline fun textField(block: TextFieldBuilder.() -> Unit): TextField {
    val textField = TextFieldBuilder()
    block.invoke(textField)
    return textField
}

inline fun passwordField(block: PasswordField.() -> Unit): PasswordField {
    val textField = PasswordField()
    block.invoke(textField)
    return textField
}

class TextFieldBuilder : TextField() {

    fun intFormatter(value: Int) {
        textFormatter = TextFormatter<Int>(IntegerStringConverter(), value)
    }

}
