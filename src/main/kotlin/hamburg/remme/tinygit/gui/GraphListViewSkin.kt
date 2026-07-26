package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.PathElement
import javafx.scene.shape.Rectangle

private const val PATH_STYLE = "graph-list-view__path"
private const val PATH_COLOR = "graph-list-view__path-color"
private const val NODE_COLOR = "graph-list-view__node-color"
private const val SPACING = 24.0
private const val RADIUS = 6.0
private const val LAST_INDEX = 9999
private const val COLOR_COUNT = 16

/**
 * Draws the Git log graph on top of [GraphListView] cells.
 *
 * Graph geometry is rebuilt only when the viewport or data changes — not on selection —
 * so selection-triggered layouts do not flash the overlay.
 *
 * @see CommitLogService.logGraph
 */
class GraphListViewSkin(
    private val control: GraphListView,
) : GraphListViewSkinBase(control) {
    private val logGraph = TinyGit.get<CommitLogService>().logGraph
    private val paths: List<Path>
    private val pathGroup = cachedGroup()
    private val circleGroup = cachedGroup()
    private val pathsClip = clipRect()
    private val circleClip = clipRect()
    private var lastViewport: Viewport? = null

    init {
        pathGroup.clip = pathsClip
        circleGroup.clip = circleClip
        children.addAll(pathGroup, circleGroup)
        paths = (0 until COLOR_COUNT).map { Path().addClass(PATH_STYLE, "$PATH_COLOR$it") }
        paths.reversed().forEach { pathGroup.children += it }
    }

    override fun layoutGraphChildren() {
        if (!control.isGraphVisible || !hasCells) {
            lastViewport = null
            paths.forEach { it.elements.clear() }
            circleGroup.children.clear()
            control.graphWidth = if (control.isGraphVisible) gutterWidth() else 0.0
            return
        }

        val viewport = currentViewport()
        if (viewport == lastViewport) return
        lastViewport = viewport

        sizeClips()
        rebuildGraph(viewport)
        control.graphWidth = gutterWidth()
    }

    private fun currentViewport() =
        Viewport(
            firstIndex = firstCell.index,
            lastIndex = lastCell.index,
            firstY = firstCell.layoutY,
            lastY = lastCell.layoutY,
            scrollX = horizontalBar.value,
            width = flow.width,
            height = flow.height,
            itemCount = skinnable.items.size,
            firstId = skinnable.items.firstOrNull()?.id,
            lastId = skinnable.items.lastOrNull()?.id,
            highestTag = logGraph.getHighestTag(),
        )

    private fun sizeClips() {
        var w = flow.width
        var h = flow.height
        if (horizontalBar.isVisible) h -= horizontalBar.height
        if (verticalBar.isVisible) w -= verticalBar.width
        pathsClip.width = w
        pathsClip.height = h
        circleClip.width = w
        circleClip.height = h
    }

    private fun rebuildGraph(viewport: Viewport) {
        val items = skinnable.items
        val scrollX = viewport.scrollX
        val first = viewport.firstIndex
        val last = viewport.lastIndex
        val cellHeight = (first..last).map { flow.getVisibleCell(it).height }.average()
        val pathElements = Array(COLOR_COUNT) { mutableListOf<PathElement>() }
        val circles = mutableListOf<Circle>()

        fun cellY(index: Int) = flow.getCell(index).let { it.layoutY + it.height / 2 }

        fun estimatedY(index: Int) = (index - first) * cellHeight

        items.forEachIndexed { index, commit ->
            val tag = logGraph.getTag(commit)
            val x = SPACING + SPACING * tag - scrollX
            // Above the viewport: estimate; otherwise use the real cell position.
            val y = if (index < first) estimatedY(index) else cellY(index)

            if (index in first..last) {
                circles += Circle(x, y, RADIUS).addClass("$NODE_COLOR${tag % COLOR_COUNT}")
            }

            commit.parents.forEach { parent ->
                val parentIndex = items.indexOfFirst { it.id == parent.id }.takeIf { it >= 0 } ?: LAST_INDEX
                val parentTag = logGraph.getTag(parent)
                if (parentTag < 0) return@forEach
                if (index < first && parentIndex < first) return@forEach
                if (index > last && parentIndex > last) return@forEach

                val px = SPACING + SPACING * parentTag - scrollX
                // Below the viewport: estimate; otherwise use the real cell position.
                val py = if (parentIndex > last) estimatedY(parentIndex) else cellY(parentIndex)
                val color = if (commit.parents.size == 1) tag % COLOR_COUNT else parentTag % COLOR_COUNT
                val e = pathElements[color]
                e += MoveTo(x, y)
                when {
                    tag == parentTag -> e += LineTo(px, py)
                    commit.parents.size == 1 && parentIndex - index > 1 -> {
                        e += LineTo(x, py - cellHeight)
                        e += CubicCurveTo(x, py, px, py - cellHeight, px, py)
                    }
                    commit.parents.size == 1 -> {
                        e += CubicCurveTo(x, y + cellHeight, px, py - cellHeight, px, py)
                    }
                    else -> {
                        e += CubicCurveTo(x, y + cellHeight, px, y, px, y + cellHeight)
                        if (parentIndex - index > 1) e += LineTo(px, py)
                    }
                }
            }
        }

        paths.forEachIndexed { i, path -> path.elements.setAll(pathElements[i]) }
        circleGroup.children.setAll(circles)
    }

    private fun gutterWidth() = SPACING / 2 + SPACING * (logGraph.getHighestTag() + 1)

    private fun cachedGroup() =
        Group().apply {
            isManaged = false
            isCache = true
            cacheHint = CacheHint.SPEED
        }

    private fun clipRect() =
        Rectangle().apply {
            isManaged = false
            isSmooth = false
        }

    /** Captures everything that affects graph geometry; equal ⇒ no rebuild needed. */
    private data class Viewport(
        val firstIndex: Int,
        val lastIndex: Int,
        val firstY: Double,
        val lastY: Double,
        val scrollX: Double,
        val width: Double,
        val height: Double,
        val itemCount: Int,
        val firstId: String?,
        val lastId: String?,
        val highestTag: Int,
    )
}
