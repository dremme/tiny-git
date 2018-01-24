package hamburg.remme.tinygit.gui.builder

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.RowConstraints
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

fun <T : Node> T.alignment(pos: Pos): T {
    StackPane.setAlignment(this, pos)
    return this
}

fun <T : Node> T.hgrow(priority: Priority): T {
    HBox.setHgrow(this, priority)
    return this
}

fun <T : Node> T.vgrow(priority: Priority): T {
    VBox.setVgrow(this, priority)
    return this
}

fun <T : Node> T.fillWidth(): T {
    if (this is Region) maxWidth = Int.MAX_VALUE.toDouble()
    GridPane.setFillWidth(this, true)
    return this
}

fun <T : Node> T.columnSpan(value: Int): T {
    GridPane.setColumnSpan(this, value)
    return this
}

inline fun stackPane(block: StackPaneBuilder.() -> Unit): StackPane {
    val pane = StackPaneBuilder()
    block.invoke(pane)
    return pane
}

inline fun splitPane(block: SplitPaneBuilder.() -> Unit): SplitPane {
    val pane = SplitPaneBuilder()
    block.invoke(pane)
    return pane
}

inline fun hbox(block: HBoxBuilder.() -> Unit): HBox {
    val box = HBoxBuilder()
    block.invoke(box)
    return box
}

inline fun vbox(block: VBoxBuilder.() -> Unit): VBox {
    val box = VBoxBuilder()
    block.invoke(box)
    return box
}

inline fun grid(numberOfColumns: Int, block: GridPaneBuilder.() -> Unit): GridPane {
    val grid = GridPaneBuilder(numberOfColumns)
    block.invoke(grid)
    return grid
}

open class StackPaneBuilder : StackPane() {

    operator fun Node.unaryPlus() {
        children.add(this)
    }

}

open class SplitPaneBuilder : SplitPane() {

    operator fun Node.unaryPlus() {
        items.add(this)
    }

}

open class HBoxBuilder : HBox() {

    operator fun Node.unaryPlus() {
        children.add(this)
    }

}

open class VBoxBuilder : VBox() {

    operator fun Node.unaryPlus() {
        children.add(this)
    }

}

class GridPaneBuilder(private val numberOfColumns: Int) : GridPane() {

    private var rowIndex = 0

    fun columns(vararg percent: Double) {
        percent.forEach {
            columnConstraints += ColumnConstraints().apply {
                percentWidth = it
                isFillWidth = true
            }
        }
    }

    fun rows(vararg percent: Double) {
        percent.forEach {
            rowConstraints += RowConstraints().apply {
                percentHeight = it
                isFillHeight = true
            }
        }
    }

    operator fun List<Node>.unaryPlus() {
        var columnIndex = 0
        forEach {
            add(it, columnIndex, rowIndex)
            columnIndex += getColumnSpan(it) ?: 1
            if (columnIndex >= numberOfColumns) {
                columnIndex = 0
                rowIndex++
            }
        }
        if (columnIndex > 0) rowIndex++
    }

}
