package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStyle
import hamburg.remme.tinygit.gui.builder.label
import javafx.collections.ListChangeListener
import javafx.scene.control.Tooltip

class StatusCountView(statusView: FileStatusView) : HBoxBuilder() {

    private val conflicting = label {
        addStyle("-fx-text-fill:#d9534f")
        graphic = FontAwesome.exclamationTriangle("#d9534f")
        tooltip = Tooltip("Conflicting")
    }
    private val added = label {
        addStyle("-fx-text-fill:#5cb85c")
        graphic = FontAwesome.plus("#5cb85c")
        tooltip = Tooltip("Added")
    }
    private val changed = label {
        addStyle("-fx-text-fill:#f0ad4e")
        graphic = FontAwesome.pencil("#f0ad4e")
        tooltip = Tooltip("Changed")
    }
    private val removed = label {
        addStyle("-fx-text-fill:#d9534f")
        graphic = FontAwesome.minus("#d9534f")
        tooltip = Tooltip("Removed")
    }
    private val missing = label {
        addStyle("-fx-text-fill:#999")
        graphic = FontAwesome.minus("#999")
        tooltip = Tooltip("Missing")
    }
    private val untracked = label {
        addStyle("-fx-text-fill:#5bc0de")
        graphic = FontAwesome.question("#5bc0de")
        tooltip = Tooltip("Untracked")
    }

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
