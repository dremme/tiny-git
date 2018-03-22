package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.tooltip
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.layout.HBox

class StatusCountView(items: ObservableList<File>) : HBox() {

    private val conflicting = label {
        addClass("status-conflict")
        managedWhen(visibleProperty())
        tooltip(I18N["status.conflicting"])
        isVisible = false
        +FileStatusView.conflictIcon()
    }
    private val added = label {
        addClass("status-added")
        managedWhen(visibleProperty())
        tooltip(I18N["status.added"])
        isVisible = false
        +FileStatusView.addedIcon()
    }
    private val copied = label {
        addClass("status-copied")
        managedWhen(visibleProperty())
        tooltip(I18N["status.copied"])
        isVisible = false
        +FileStatusView.copiedIcon()
    }
    private val renamed = label {
        addClass("status-renamed")
        managedWhen(visibleProperty())
        tooltip(I18N["status.renamed"])
        isVisible = false
        +FileStatusView.renamedIcon()
    }
    private val modified = label {
        addClass("status-modified")
        managedWhen(visibleProperty())
        tooltip(I18N["status.modified"])
        isVisible = false
        +FileStatusView.modifiedIcon()
    }
    private val removed = label {
        addClass("status-removed")
        managedWhen(visibleProperty())
        tooltip(I18N["status.removed"])
        isVisible = false
        +FileStatusView.removedIcon()
    }
    private val missing = label {
        addClass("status-missing")
        managedWhen(visibleProperty())
        tooltip(I18N["status.missing"])
        isVisible = false
        +FileStatusView.missingIcon()
    }
    private val untracked = label {
        addClass("status-untracked")
        managedWhen(visibleProperty())
        tooltip(I18N["status.untracked"])
        isVisible = false
        +FileStatusView.untrackedIcon()
    }

    init {
        addClass("status-count-view")
        children.addAll(conflicting, added, copied, renamed, modified, removed, missing, untracked)
        items.addListener(ListChangeListener { update(it.list) })
    }

    private fun update(files: List<File>) {
        conflicting.update(files, { it.status == File.Status.CONFLICT })
        added.update(files, { it.status == File.Status.ADDED && it.isCached })
        untracked.update(files, { it.status == File.Status.ADDED && !it.isCached })
        copied.update(files, { it.status == File.Status.COPIED })
        renamed.update(files, { it.status == File.Status.RENAMED })
        modified.update(files, { it.status == File.Status.MODIFIED })
        removed.update(files, { it.status == File.Status.REMOVED && it.isCached })
        missing.update(files, { it.status == File.Status.REMOVED && !it.isCached })
    }

    private fun Label.update(files: List<File>, predicate: (File) -> Boolean) {
        val count = files.filter(predicate).size
        text = count.toString()
        isVisible = count > 0
    }

}
