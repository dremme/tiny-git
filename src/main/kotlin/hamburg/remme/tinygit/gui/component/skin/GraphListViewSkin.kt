package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.GraphListView
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

private const val EMPTY_SPACING = 0.0
private const val SPACING = 24.0
private const val RADIUS = 6.0
private const val LAST_INDEX = 9999
private const val COLOR_COUNT = 11

/**
 * This skin is enhancing the [javafx.scene.control.skin.ListViewSkin] to display a Git log graph style [Path].
 * The cells are still drawn by the default list skin.
 *
 * @todo: graph clipping over scrollbars
 *
 * @see hamburg.remme.tinygit.domain.LogGraph
 */
class GraphListViewSkin(private val graphView: GraphListView) : GraphListViewSkinBase(graphView) {

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

    override fun layoutGraphChildren() {
        paths.forEach { it.elements.clear() }
        circleGroup.children.clear()

        if (graphView.isGraphVisible && hasCells) {
            val scrollX = horizontalBar.value
            val cellHeight = (firstCell.index..lastCell.index).map { flow.getVisibleCell(it).height }.average()

            graphView.items.forEachIndexed { commitIndex, commit ->
                val tag = graphView.logGraph.getTag(commit)

                val commitX = SPACING + SPACING * tag - scrollX
                val commitY = if (commitIndex < firstCell.index) {
                    (commitIndex - firstCell.index) * cellHeight
                } else {
                    flow.getCell(commitIndex).let { it.layoutY + it.height / 2 }
                }

                if (commitIndex >= firstCell.index && commitIndex <= lastCell.index) {
                    circleGroup.children += Circle(commitX, commitY, RADIUS).addClass("commit-node", "node-color${tag % COLOR_COUNT}")
                }

                commit.parents.forEach { parent ->
                    val parentIndex = graphView.items.indexOfFirst { it.id == parent.id }.takeIf { it >= 0 } ?: LAST_INDEX
                    val parentTag = graphView.logGraph.getTag(parent)

                    if (parentTag >= 0
                            && !(commitIndex < firstCell.index && parentIndex < firstCell.index)
                            && !(commitIndex > lastCell.index && parentIndex > lastCell.index)) {
                        val parentX = SPACING + SPACING * parentTag - scrollX
                        val parentY = if (parentIndex > lastCell.index) {
                            (parentIndex - firstCell.index) * cellHeight
                        } else {
                            flow.getCell(parentIndex).let { it.layoutY + it.height / 2 }
                        }

                        val path = paths[if (commit.parents.size == 1) tag % COLOR_COUNT else parentTag % COLOR_COUNT]
                        path.elements += MoveTo(commitX, commitY)
                        when {
                            tag == parentTag -> {
                                path.elements += LineTo(parentX, parentY)
                            }
                            commit.parents.size == 1 -> {
                                if (parentIndex - commitIndex > 1) {
                                    path.elements += LineTo(commitX, parentY - cellHeight)
                                    path.elements += CubicCurveTo(commitX, parentY, parentX, parentY - cellHeight, parentX, parentY)
                                } else {
                                    path.elements += CubicCurveTo(commitX, commitY + cellHeight, parentX, parentY - cellHeight, parentX, parentY)
                                }
                            }
                            else -> {
                                path.elements += CubicCurveTo(commitX, commitY + cellHeight, parentX, commitY, parentX, commitY + cellHeight)
                                if (parentIndex - commitIndex > 1) path.elements += LineTo(parentX, parentY)
                            }
                        }
                    }
                }
            }
            graphView.graphWidth = SPACING + SPACING * (graphView.logGraph.getHighestTag() + 1)
        } else if (!hasCells) {
            graphView.graphWidth = SPACING + SPACING * (graphView.logGraph.getHighestTag() + 1)
        } else {
            graphView.graphWidth = EMPTY_SPACING
        }
    }

}
