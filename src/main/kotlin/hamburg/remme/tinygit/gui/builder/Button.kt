package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.gui.Action
import hamburg.remme.tinygit.gui.keyCombinationText
import javafx.scene.control.Button
import javafx.scene.control.Tooltip

fun button(action: Action) = button {
    text = action.text
    graphic = action.icon.invoke()
    action.shortcut?.keyCombinationText()?.let { if (it.isNotBlank()) tooltip = Tooltip(it) }
    action.disable?.let { disabledWhen(it) }
    setOnAction { action.handler.invoke() }
}

inline fun button(block: Button.() -> Unit): Button {
    val button = Button()
    block.invoke(button)
    return button
}
