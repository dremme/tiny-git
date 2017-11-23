package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

fun Node.vgrow(priority: Priority) {
    VBox.setVgrow(this, priority)
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
