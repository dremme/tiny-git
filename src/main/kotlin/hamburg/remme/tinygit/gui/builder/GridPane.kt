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

inline fun grid(width: Int, block: GridPaneBuilder.() -> Unit): GridPane {
    val grid = GridPaneBuilder(width)
    block.invoke(grid)
    return grid
}

class GridPaneBuilder(private val width: Int) : GridPane() {

    private var rowIndex = 0

    operator fun List<Node>.unaryPlus() {
        var columnIndex = 0
        forEach {
            add(it, columnIndex, rowIndex)
            columnIndex += getColumnSpan(it) ?: 1
            if (columnIndex >= width) {
                columnIndex = 0
                rowIndex++
            }
        }
        if (columnIndex > 0) rowIndex++
    }

}
