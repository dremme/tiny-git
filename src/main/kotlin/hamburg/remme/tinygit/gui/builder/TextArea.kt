package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

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
