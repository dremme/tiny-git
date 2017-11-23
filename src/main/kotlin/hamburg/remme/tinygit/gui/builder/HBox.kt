package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.layout.HBox

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
