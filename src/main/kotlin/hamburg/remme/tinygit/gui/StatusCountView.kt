package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.collections.ListChangeListener
import javafx.scene.control.Tooltip

class StatusCountView(statusView: FileStatusView) : HBoxBuilder() {

    private val conflicting = label {
        addClass("status-conflict")
        graphic = FileStatusView.conflictIcon()
        tooltip = Tooltip("Conflicting")
    }
    private val added = label {
        addClass("status-added")
        graphic = FileStatusView.addedIcon()
        tooltip = Tooltip("Added")
    }
    private val copied = label {
        addClass("status-copied")
        graphic = FileStatusView.copiedIcon()
        tooltip = Tooltip("Copied")
    }
    private val renamed = label {
        addClass("status-renamed")
        graphic = FileStatusView.renamedIcon()
        tooltip = Tooltip("Renamed")
    }
    private val changed = label {
        addClass("status-changed")
        graphic = FileStatusView.changedIcon()
        tooltip = Tooltip("Changed")
    }
    private val removed = label {
        addClass("status-removed")
        graphic = FileStatusView.removedIcon()
        tooltip = Tooltip("Removed")
    }
    private val missing = label {
        addClass("status-missing")
        graphic = FileStatusView.missingIcon()
        tooltip = Tooltip("Missing")
    }
    private val untracked = label {
        addClass("status-untracked")
        graphic = FileStatusView.untrackedIcon()
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
        val copiedCount = counts[LocalFile.Status.COPIED] ?: 0
        val renamedCount = counts[LocalFile.Status.RENAMED] ?: 0
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
        if (copiedCount > 0) {
            copied.text = copiedCount.toString()
            +copied
        }
        if (renamedCount > 0) {
            renamed.text = renamedCount.toString()
            +renamed
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
