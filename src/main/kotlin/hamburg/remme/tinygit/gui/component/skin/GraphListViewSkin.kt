package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.CommitIsh
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.GraphListView
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

class GraphListViewSkin(private val graphList: GraphListView) : GraphListViewSkinBase(graphList) {

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

    override fun layoutGraphChildren() {
        paths.forEach { it.elements.clear() }
        circleGroup.children.clear()

        val firstCell = flow.firstVisibleCell
        val lastCell = flow.lastVisibleCell

        if (graphList.isGraphVisible && firstCell != null && lastCell != null) {
            val cellHeight = (firstCell.index..lastCell.index).map { flow.getVisibleCell(it).height }.average()

            val branches = mutableListOf<BranchFlow>()
            graphList.items.forEachIndexed { i, it -> branches.createFlow(it, i) }
            graphList.items.forEachIndexed { commitIndex, commit ->
                val tag = branches.getTag(commit)

                val commitX = SPACING + SPACING * tag
                val commitY = if (commitIndex < firstCell.index) {
                    (commitIndex - firstCell.index) * cellHeight
                } else {
                    flow.getCell(commitIndex).let { it.layoutY + it.height / 2 }
                }

                if (commitIndex >= firstCell.index && commitIndex <= lastCell.index) {
                    circleGroup.children += Circle(commitX, commitY, RADIUS).addClass("commit-node", "node-color${tag % 8}")
                }

                commit.parents.forEach { parent ->
                    val parentIndex = graphList.items.indexOfFirst { it.id == parent.id }.takeIf { it >= 0 } ?: 9999
                    val parentTag = branches.getTag(parent)

                    if (!(commitIndex < firstCell.index && parentIndex < firstCell.index)
                            && !(commitIndex > lastCell.index && parentIndex > lastCell.index)) {
                        val parentX = SPACING + SPACING * parentTag
                        val parentY = if (parentIndex > lastCell.index) {
                            (parentIndex - firstCell.index) * cellHeight
                        } else {
                            flow.getCell(parentIndex).let { it.layoutY + it.height / 2 }
                        }

                        val path = paths[if (commit.parents.size == 1) tag % 8 else parentTag % 8]
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
            graphList.graphWidth = SPACING + SPACING * branches.map { it.tag + 1 }.max()!!
        } else {
            graphList.graphWidth = 0.0
        }
    }

    private fun List<BranchFlow>.containsCommit(commit: Commit) = any { it.contains(commit.id) }

    private fun List<BranchFlow>.containsCommit(commit: CommitIsh) = any { it.contains(commit.id) }

    private fun MutableList<BranchFlow>.getTag(commit: Commit) = find { it.any { it == commit.id } }!!.tag

    private fun MutableList<BranchFlow>.getTag(commit: CommitIsh) = find { it.any { it == commit.id } }!!.tag

    private fun MutableList<BranchFlow>.createFlow(commit: Commit, commitIndex: Int) {
        if (!containsCommit(commit)) {
            val tags = filter { it.start <= commitIndex && it.end >= commitIndex }.map { it.tag }
            val max = tags.max() ?: 0
            val tag = (0..max).firstOrNull { !tags.contains(it) } ?: max+1

            val branch = BranchFlow(tag, commitIndex)
            add(branch)

            commit.more(branch, this)
        }
    }

    private fun Commit.more(branch: BranchFlow, branches: List<BranchFlow>) {
        branch.add(id)
        parents.firstOrNull()
                ?.let {
                    if (!branches.containsCommit(it)) it.peel()?.more(branch, branches) ?: branch.indeterminate(it)
                    else branch.finish(it)
                }
                ?: branch.finish(this)
    }

    private fun BranchFlow.finish(commit: Commit) {
        end = graphList.items.indexOf(commit)
    }

    private fun BranchFlow.finish(commit: CommitIsh) {
        end = commit.peel()?.let { graphList.items.indexOf(it) } ?: 9999
    }

    private fun BranchFlow.indeterminate(commit: CommitIsh) {
        add(commit.id)
    }

    private fun CommitIsh.peel() = graphList.items.firstOrNull { it.id == id }

    private inner class BranchFlow(val tag: Int, val start: Int, var end: Int = 9999) : ArrayList<String>()

}
