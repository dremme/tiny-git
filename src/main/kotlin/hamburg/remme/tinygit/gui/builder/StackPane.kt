package hamburg.remme.tinygit.gui.builder

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.StackPane

fun Node.alignment(pos: Pos) {
    StackPane.setAlignment(this, pos)
}

inline fun stackPane(block: StackPaneBuilder.() -> Unit): StackPane {
    val pane = StackPaneBuilder()
    block.invoke(pane)
    return pane
}

open class StackPaneBuilder : StackPane() {

    operator fun Node.unaryPlus() {
        children.add(this)
    }

}
