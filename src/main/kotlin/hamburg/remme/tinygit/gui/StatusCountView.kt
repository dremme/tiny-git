package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.collections.ListChangeListener
import javafx.scene.control.Tooltip

// TODO: use visibility and managed here
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
    private val modified = label {
        addClass("status-modified")
        graphic = FileStatusView.modifiedIcon()
        tooltip = Tooltip("Modified")
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

    // TODO: use size not count()
    private fun fetchStatus(files: List<LocalFile>) {
        val conflictingCount = files.filter { it.status == LocalFile.Status.CONFLICT }.count()
        val addedCount = files.filter { it.status == LocalFile.Status.ADDED && it.cached }.count()
        val untrackedCount = files.filter { it.status == LocalFile.Status.ADDED && !it.cached }.count()
        val copiedCount = files.filter { it.status == LocalFile.Status.COPIED }.count()
        val renamedCount = files.filter { it.status == LocalFile.Status.RENAMED }.count()
        val modifiedCount = files.filter { it.status == LocalFile.Status.MODIFIED }.count()
        val removedCount = files.filter { it.status == LocalFile.Status.REMOVED && it.cached }.count()
        val missingCount = files.filter { it.status == LocalFile.Status.REMOVED && !it.cached }.count()

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
        if (modifiedCount > 0) {
            modified.text = modifiedCount.toString()
            +modified
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
