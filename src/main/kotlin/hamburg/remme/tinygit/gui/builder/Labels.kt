package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.control.Label

inline fun label(block: LabelBuilder.() -> Unit): Label {
    val label = LabelBuilder()
    block.invoke(label)
    return label
}

open class LabelBuilder : Label() {

    operator fun String.unaryPlus() {
        text = this
    }

    operator fun Node.unaryPlus() {
        graphic = this
    }

}
