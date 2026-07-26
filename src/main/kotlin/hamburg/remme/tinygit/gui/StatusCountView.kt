package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.execute
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.tooltip
import hamburg.remme.tinygit.gui.component.Icons
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.control.Label

private const val DEFAULT_STYLE_CLASS = "status-count-view"
private const val LINES_STYLE_CLASS = "status-lines"
private const val LINES_ADDED_STYLE_CLASS = "status-lines-added"
private const val LINES_REMOVED_STYLE_CLASS = "status-lines-removed"

/**
 * A list of [File]s which status will be displayed as summary. Often used inside a
 * [javafx.scene.control.ToolBar] and in conjunction with the [FileStatusView].
 *
 * When [numStatsLoader] is provided, also shows total lines added and removed for the
 * current file set (loaded asynchronously).
 */
class StatusCountView(
    items: ObservableList<File>,
    private val numStatsLoader: (() -> List<NumStat>),
) : HBoxBuilder() {
    private val conflicting =
        label {
            addClass(CONFLICT_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.conflicting"])
            isVisible = false
            graphic = FileStatusView.conflictIcon()
        }
    private val added =
        label {
            addClass(ADDED_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.added"])
            isVisible = false
            graphic = FileStatusView.addedIcon()
        }
    private val copied =
        label {
            addClass(COPIED_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.copied"])
            isVisible = false
            graphic = FileStatusView.copiedIcon()
        }
    private val renamed =
        label {
            addClass(RENAMED_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.renamed"])
            isVisible = false
            graphic = FileStatusView.renamedIcon()
        }
    private val modified =
        label {
            addClass(MODIFIED_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.modified"])
            isVisible = false
            graphic = FileStatusView.modifiedIcon()
        }
    private val removed =
        label {
            addClass(REMOVED_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.removed"])
            isVisible = false
            graphic = FileStatusView.removedIcon()
        }
    private val missing =
        label {
            addClass(MISSING_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.missing"])
            isVisible = false
            graphic = FileStatusView.missingIcon()
        }
    private val untracked =
        label {
            addClass(UNTRACKED_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.untracked"])
            isVisible = false
            graphic = FileStatusView.untrackedIcon()
        }
    private val linesAdded =
        label {
            addClass(LINES_ADDED_STYLE_CLASS)
        }
    private val linesRemoved =
        label {
            addClass(LINES_REMOVED_STYLE_CLASS)
        }
    private val lineStats =
        hbox {
            addClass(LINES_STYLE_CLASS)
            managedWhen(visibleProperty())
            tooltip(I18N["status.linesChanged"])
            isVisible = false
            +Icons.alignLeft()
            +linesAdded
            +linesRemoved
        }
    private var lineCountTask: Task<*>? = null

    init {
        addClass(DEFAULT_STYLE_CLASS)
        +conflicting
        +added
        +copied
        +renamed
        +modified
        +removed
        +missing
        +untracked
        +lineStats
        items.addListener(ListChangeListener { update(it.list) })
        if (items.isNotEmpty()) update(items)
    }

    private fun update(files: List<File>) {
        conflicting.update(files) { it.status == File.Status.CONFLICT }
        added.update(files) { it.status == File.Status.ADDED && it.isCached }
        untracked.update(files) { it.status == File.Status.ADDED && !it.isCached }
        copied.update(files) { it.status == File.Status.COPIED }
        renamed.update(files) { it.status == File.Status.RENAMED }
        modified.update(files) { it.status == File.Status.MODIFIED }
        removed.update(files) { it.status == File.Status.REMOVED && it.isCached }
        missing.update(files) { it.status == File.Status.REMOVED && !it.isCached }
        updateLineCounts(files)
    }

    private fun updateLineCounts(files: List<File>) {
        lineCountTask?.cancel()
        if (files.isEmpty()) {
            lineStats.isVisible = false
            return
        }
        lineCountTask =
            object : Task<List<NumStat>>() {
                override fun call() = numStatsLoader.invoke()

                override fun succeeded() {
                    val (addedCount, removedCount) = value.sumOf { it.added } to value.sumOf { it.removed }
                    linesAdded.text = "+$addedCount"
                    linesRemoved.text = "-$removedCount"
                    lineStats.isVisible = addedCount > 0 || removedCount > 0
                }

                override fun failed() {
                    lineStats.isVisible = false
                }
            }.execute()
    }

    private fun Label.update(
        files: List<File>,
        predicate: (File) -> Boolean,
    ) {
        val count = files.filter(predicate).size
        text = count.toString()
        isVisible = count > 0
    }
}
