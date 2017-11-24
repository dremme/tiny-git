package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.collections.ListChangeListener

class StatusCountView(statusView: FileStatusView) : HBoxBuilder() {

    private val conflicting = _label(tooltip = "Conflicting", icon = FontAwesome.exclamationTriangle("#d9534f"), color = "#d9534f")
    private val added = _label(tooltip = "Added", icon = FontAwesome.plus("#5cb85c"), color = "#5cb85c")
    private val changed = _label(tooltip = "Changed", icon = FontAwesome.pencil("#f0ad4e"), color = "#f0ad4e")
    private val removed = _label(tooltip = "Removed", icon = FontAwesome.minus("#d9534f"), color = "#d9534f")
    private val missing = _label(tooltip = "Missing", icon = FontAwesome.minus("#999"), color = "#999")
    private val untracked = _label(tooltip = "Untracked", icon = FontAwesome.question("#5bc0de"), color = "#5bc0de")

    init {
        addClass("status-count-view")
        statusView.items.addListener(ListChangeListener { fetchStatus(it.list) })
        fetchStatus(statusView.items)
    }

    private fun fetchStatus(files: List<LocalFile>) {
        val counts = files.groupingBy { it.status }.eachCount()
        val conflictingCount = counts[LocalFile.Status.CONFLICT] ?: 0
        val addedCount = counts[LocalFile.Status.ADDED] ?: 0
        val changedCount = (counts[LocalFile.Status.MODIFIED] ?: 0) + (counts[LocalFile.Status.CHANGED] ?: 0)
        val removedCount = counts[LocalFile.Status.REMOVED] ?: 0
        val missingCount = counts[LocalFile.Status.MISSING] ?: 0
        val untrackedCount = counts[LocalFile.Status.UNTRACKED] ?: 0

        children.clear()

        if (conflictingCount > 0) {
            conflicting.text = conflictingCount.toString()
            +conflicting
        }
        if (addedCount > 0) {
            added.text = addedCount.toString()
            +added
        }
        if (changedCount > 0) {
            changed.text = changedCount.toString()
            +changed
        }
        if (removedCount > 0) {
            removed.text = removedCount.toString()
            +removed
        }
        if (missingCount > 0) {
            missing.text = missingCount.toString()
            +missing
        }
        if (untrackedCount > 0) {
            untracked.text = untrackedCount.toString()
            +untracked
        }
    }

}
