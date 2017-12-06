package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCombination

fun button(action: Action) = button {
    text = action.text
    graphic = action.icon.invoke()
    action.shortcut?.let { KeyCombination.valueOf(it).displayText }?.let { tooltip = Tooltip(it) }
    action.disable?.let { disabledWhen(it) }
    setOnAction { action.handler.invoke() }
}

inline fun button(block: Button.() -> Unit): Button {
    val button = Button()
    block.invoke(button)
    return button
}

inline fun link(block: Hyperlink.() -> Unit): Hyperlink {
    val link = Hyperlink()
    block.invoke(link)
    return link
}
