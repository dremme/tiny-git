package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.ComboBox

inline fun <T> comboBox(block: ComboBox<T>.() -> Unit): ComboBox<T> {
    val comboBox = ComboBox<T>()
    block.invoke(comboBox)
    return comboBox
}
