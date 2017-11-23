package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.control.SplitPane

inline fun splitPane(block: SplitPaneBuilder.() -> Unit): SplitPane {
    val pane = SplitPaneBuilder()
    block.invoke(pane)
    return pane
}

open class SplitPaneBuilder : SplitPane() {

    operator fun Node.unaryPlus() {
        items.add(this)
    }

}
