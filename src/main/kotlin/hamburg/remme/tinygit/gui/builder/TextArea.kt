package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.TextArea

inline fun textArea(block: TextArea.() -> Unit): TextArea {
    val textArea = TextArea()
    block.invoke(textArea)
    return textArea
}
