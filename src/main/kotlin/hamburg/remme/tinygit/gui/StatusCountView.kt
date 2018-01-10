package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.domain.LocalFile
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import javafx.collections.ListChangeListener
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox

class StatusCountView(statusView: FileStatusView) : HBox() {

    private val conflicting = label {
        addClass("status-conflict")
        managedWhen(visibleProperty())
        graphic = FileStatusView.conflictIcon()
        tooltip = Tooltip("Conflicting")
        isVisible = false
    }
    private val added = label {
        addClass("status-added")
        managedWhen(visibleProperty())
        graphic = FileStatusView.addedIcon()
        tooltip = Tooltip("Added")
        isVisible = false
    }
    private val copied = label {
        addClass("status-copied")
        managedWhen(visibleProperty())
        graphic = FileStatusView.copiedIcon()
        tooltip = Tooltip("Copied")
        isVisible = false
    }
    private val renamed = label {
        addClass("status-renamed")
        managedWhen(visibleProperty())
        graphic = FileStatusView.renamedIcon()
        tooltip = Tooltip("Renamed")
        isVisible = false
    }
    private val modified = label {
        addClass("status-modified")
        managedWhen(visibleProperty())
        graphic = FileStatusView.modifiedIcon()
        tooltip = Tooltip("Modified")
        isVisible = false
    }
    private val removed = label {
        addClass("status-removed")
        managedWhen(visibleProperty())
        graphic = FileStatusView.removedIcon()
        tooltip = Tooltip("Removed")
        isVisible = false
    }
    private val missing = label {
        addClass("status-missing")
        managedWhen(visibleProperty())
        graphic = FileStatusView.missingIcon()
        tooltip = Tooltip("Missing")
        isVisible = false
    }
    private val untracked = label {
        addClass("status-untracked")
        managedWhen(visibleProperty())
        graphic = FileStatusView.untrackedIcon()
        tooltip = Tooltip("Untracked")
        isVisible = false
    }

    init {
        addClass("status-count-view")
        children.addAll(conflicting, added, copied, renamed, modified, removed, missing, untracked)
        statusView.items.addListener(ListChangeListener { update(it.list) })
    }

    private fun update(files: List<LocalFile>) {
        conflicting.update(files, { it.status == LocalFile.Status.CONFLICT })
        added.update(files, { it.status == LocalFile.Status.ADDED && it.cached })
        untracked.update(files, { it.status == LocalFile.Status.ADDED && !it.cached })
        copied.update(files, { it.status == LocalFile.Status.COPIED })
        renamed.update(files, { it.status == LocalFile.Status.RENAMED })
        modified.update(files, { it.status == LocalFile.Status.MODIFIED })
        removed.update(files, { it.status == LocalFile.Status.REMOVED && it.cached })
        missing.update(files, { it.status == LocalFile.Status.REMOVED && !it.cached })
    }

    private fun Label.update(files: List<LocalFile>, predicate: (LocalFile) -> Boolean) {
        val count = files.filter(predicate).size
        text = count.toString()
        isVisible = count > 0
    }

}
