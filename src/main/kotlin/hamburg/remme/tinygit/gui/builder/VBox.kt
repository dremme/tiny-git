package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

fun <T : Node> T.vgrow(priority: Priority): T {
    VBox.setVgrow(this, priority)
    return this
}

inline fun vbox(block: VBoxBuilder.() -> Unit): VBox {
    val box = VBoxBuilder()
    block.invoke(box)
    return box
}

open class VBoxBuilder : VBox() {

    operator fun Node.unaryPlus() {
        children.add(this)
    }

}
