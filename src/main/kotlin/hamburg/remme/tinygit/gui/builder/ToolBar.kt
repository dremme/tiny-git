package hamburg.remme.tinygit.gui.builder

import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableIntegerValue
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority

inline fun toolBar(block: ToolBarBuilder.() -> Unit): ToolBar {
    val toolBar = ToolBarBuilder()
    block.invoke(toolBar)
    return toolBar
}

class ToolBarBuilder : ToolBar() {

    fun addSpacer() {
        +Pane().hgrow(Priority.ALWAYS)
    }

    operator fun Node.unaryPlus() {
        items.add(this)
    }

    operator fun Action.unaryPlus() {
        +button(this)
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +Separator()
        action.forEach {
            +stackPane {
                +button(it)
                if (it.count != null) +label {
                    addClass("count-badge")
                    alignment(Pos.TOP_RIGHT)
                    visibleWhen(Bindings.notEqual(0, it.count))
                    textProperty().bind(badge(it.count))
                }
            }
        }
    }

    private fun badge(count: ObservableIntegerValue)
            = Bindings.createStringBinding({ if (count.get() > 0) count.get().toString() else "*" }, arrayOf(count))!!

}
