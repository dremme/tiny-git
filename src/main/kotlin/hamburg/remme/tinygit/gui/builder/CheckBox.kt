package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.CheckBox

inline fun checkBox(block: CheckBox.() -> Unit): CheckBox {
    val checkBox = CheckBox()
    block.invoke(checkBox)
    return checkBox
}
