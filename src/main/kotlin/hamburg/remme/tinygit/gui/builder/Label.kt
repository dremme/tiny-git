package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.Label

inline fun label(block: Label.() -> Unit): Label {
    val label = Label()
    block.invoke(label)
    return label
}
