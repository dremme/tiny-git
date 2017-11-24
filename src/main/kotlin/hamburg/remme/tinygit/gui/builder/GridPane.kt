package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.layout.GridPane

fun <T : Node> T.fillWidth(): T {
    GridPane.setFillWidth(this, true)
    return this
}

fun <T : Node> T.columnSpan(value: Int): T {
    GridPane.setColumnSpan(this, value)
    return this
}

fun <T : Node> T.rowSpan(value: Int): T {
    GridPane.setRowSpan(this, value)
    return this
}

inline fun grid(block: GridPaneBuilder.() -> Unit): GridPane {
    val grid = GridPaneBuilder()
    block.invoke(grid)
    return grid
}

class GridPaneBuilder : GridPane() {

    private var row = 0

    fun addRow(vararg node: Node) {
        addRow(row++, *node)
    }

}
