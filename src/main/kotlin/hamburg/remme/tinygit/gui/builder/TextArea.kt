package hamburg.remme.tinygit.gui.builder

import com.sun.javafx.scene.traversal.Direction
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode

inline fun textArea(block: TextArea.() -> Unit): TextArea {
    val textArea = TextArea()
    textArea.setOnKeyPressed {
        if (it.code == KeyCode.TAB && !it.isShortcutDown && !it.isShiftDown) {
            textArea.impl_traverse(Direction.NEXT)
            it.consume()
        }
    }
    block.invoke(textArea)
    return textArea
}
