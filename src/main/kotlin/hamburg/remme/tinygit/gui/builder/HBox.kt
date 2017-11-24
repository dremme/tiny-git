package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority

fun <T : Node> T.hgrow(priority: Priority): T {
    HBox.setHgrow(this, priority)
    return this
}

inline fun hbox(block: HBoxBuilder.() -> Unit): HBox {
    val box = HBoxBuilder()
    block.invoke(box)
    return box
}

open class HBoxBuilder : HBox() {

    operator fun Node.unaryPlus() {
        children.add(this)
    }

}
