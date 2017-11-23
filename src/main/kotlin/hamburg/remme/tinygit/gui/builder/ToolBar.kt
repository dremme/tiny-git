package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.gui.ActionGroup
import hamburg.remme.tinygit.gui._button
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableIntegerValue
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar

inline fun toolBar(block: ToolBarBuilder.() -> Unit): ToolBar {
    val toolBar = ToolBarBuilder()
    block.invoke(toolBar)
    return toolBar
}

open class ToolBarBuilder : ToolBar() {

    operator fun Node.unaryPlus() {
        items.add(this)
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +Separator()
        action.forEach {
            +stackPane {
                +_button(it)
                if (it.count != null) +label {
                    addClass("count-badge")
                    alignment(Pos.TOP_RIGHT)

                    textProperty().bind(badge(it.count))
                    visibleProperty().bind(Bindings.notEqual(0, it.count))
                }
            }
        }
    }

    private fun badge(count: ObservableIntegerValue)
            = Bindings.createStringBinding({ if (count.get() > 0) count.get().toString() else "*" }, arrayOf(count))!!

}
