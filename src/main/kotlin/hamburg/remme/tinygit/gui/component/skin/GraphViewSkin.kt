package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.gui.GraphView
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

class GraphViewSkin(private val control: GraphView) : GraphViewSkinBase(control) {

    private val SPACING = 24.0
    private val RADIUS = 6.0
    private val paths: List<Path>
    private val circleGroup = Group()

    init {
        val pathGroup = Group()
        pathGroup.isManaged = false
        circleGroup.isManaged = false
        children.addAll(pathGroup, circleGroup)
        paths = (0..7).map { Path().addClass("commit-path", "path-color$it") }
        paths.reversed().forEach { pathGroup.children += it }
    }

    // TODO: Inefficient and buggy; needs to be fully implemented
    override fun layoutGraphChildren() {
        paths.forEach { it.elements.clear() }
        circleGroup.children.clear()

        val firstCell = flow.firstVisibleCell
        val lastCell = flow.lastVisibleCell

        if (control.graphVisible.get() && firstCell != null && lastCell != null) {
            val graph = control.commitGraph.get()

            val h = (firstCell.index..lastCell.index).map { flow.getVisibleCell(it).height }.average()

            graph.forEachIndexed { i, node ->
                val path = paths[node.tag % 8]

                val nx = SPACING + SPACING * node.tag
                val ny = if (i < firstCell.index) i * h - firstCell.index * h else flow.getCell(i).let { it.layoutY + it.height / 2 }

                node.parents.forEach { parent ->
                    val pi = graph.indexOf(parent)

                    if ((i >= firstCell.index || pi >= firstCell.index) && (i <= lastCell.index || pi <= lastCell.index)) {
                        val px = SPACING + SPACING * parent.tag
                        val py = if (pi > lastCell.index) pi * h - firstCell.index * h else flow.getCell(pi).let { it.layoutY + it.height / 2 }

                        path.elements += MoveTo(nx, ny)
                        when {
                            node.tag == parent.tag -> {
                                path.elements += LineTo(px, py)
                            }
                            node.parents.size == 1 -> {
                                if (pi - i > 1) {
                                    path.elements += LineTo(nx, py - h)
                                    path.elements += CubicCurveTo(nx, py, px, py - h, px, py)
                                } else {
                                    path.elements += CubicCurveTo(nx, ny + h, px, py - h, px, py)
                                }
                            }
                            else -> {
                                path.elements += CubicCurveTo(nx, ny + h, px, ny, px, ny + h)
                            }
                        }
                    }
                }
                if (i >= firstCell.index && i <= lastCell.index) {
                    circleGroup.children += Circle(nx, ny, RADIUS).addClass("commit-node", "node-color${node.tag % 8}")
                }
            }
            control.graphWidth = SPACING + SPACING * graph.groupBy { it.tag }.count()
        } else {
            control.graphWidth = 0.0
        }
    }

}
