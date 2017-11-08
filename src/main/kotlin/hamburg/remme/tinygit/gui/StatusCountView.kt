package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.FontAwesome.EXCLAMATION_TRIANGLE
import hamburg.remme.tinygit.gui.FontAwesome.MINUS
import hamburg.remme.tinygit.gui.FontAwesome.PENCIL
import hamburg.remme.tinygit.gui.FontAwesome.PLUS
import hamburg.remme.tinygit.gui.FontAwesome.QUESTION
import javafx.collections.ListChangeListener
import javafx.scene.layout.HBox

class StatusCountView(statusView: FileStatusView) : HBox() {

    private val conflicting = label(tooltip = "Conflicting", icon = EXCLAMATION_TRIANGLE("#d9534f"), color = "#d9534f")
    private val added = label(tooltip = "Added", icon = PLUS("#5cb85c"), color = "#5cb85c")
    private val changed = label(tooltip = "Changed", icon = PENCIL("#f0ad4e"), color = "#f0ad4e")
    private val removed = label(tooltip = "Removed", icon = MINUS("#d9534f"), color = "#d9534f")
    private val missing = label(tooltip = "Missing", icon = MINUS("#999"), color = "#999")
    private val untracked = label(tooltip = "Untracked", icon = QUESTION("#5bc0de"), color = "#5bc0de")

    init {
        styleClass += "status-count-view"
        children.addAll(conflicting, added, changed, removed, missing, untracked)
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

        if (conflictingCount == 0) children.remove(conflicting)
        else if (!children.contains(conflicting)) children += conflicting
        conflicting.text = conflictingCount.toString()

        if (addedCount == 0) children.remove(added)
        else if (!children.contains(added)) children += added
        added.text = addedCount.toString()

        if (changedCount == 0) children.remove(changed)
        else if (!children.contains(changed)) children += changed
        changed.text = changedCount.toString()

        if (removedCount == 0) children.remove(removed)
        else if (!children.contains(removed)) children += removed
        removed.text = removedCount.toString()

        if (missingCount == 0) children.remove(missing)
        else if (!children.contains(missing)) children += missing
        missing.text = missingCount.toString()

        if (untrackedCount == 0) children.remove(untracked)
        else if (!children.contains(untracked)) children += untracked
        untracked.text = untrackedCount.toString()
    }

}
