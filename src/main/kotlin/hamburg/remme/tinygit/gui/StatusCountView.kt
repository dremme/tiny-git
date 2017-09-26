package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import javafx.collections.ListChangeListener
import javafx.scene.control.Label
import javafx.scene.layout.HBox

class StatusCountView(statusView: FileStatusView) : HBox() {

    private val added = Label("", FontAwesome.plus().also { it.style = "-fx-text-fill:#5cb85c" })
    private val changed = Label("", FontAwesome.pencil().also { it.style = "-fx-text-fill:#f0ad4e" })
    private val removed = Label("", FontAwesome.minus().also { it.style = "-fx-text-fill:#d9534f" })
    private val untracked = Label("", FontAwesome.question().also { it.style = "-fx-text-fill:#5bc0de" })

    init {
        styleClass += "status-count-view"
        children.addAll(added, changed, removed, untracked)
        statusView.items.addListener(ListChangeListener { fetchStatus(it.list) })
        fetchStatus(statusView.items)
    }

    private fun fetchStatus(files: List<LocalFile>) {
        val counts = files.groupingBy { it.status }.eachCount()
        val addedCount = counts[LocalFile.Status.ADDED] ?: 0
        val changedCount = (counts[LocalFile.Status.MODIFIED] ?: 0) + (counts[LocalFile.Status.CHANGED] ?: 0)
        val removedCount = counts[LocalFile.Status.REMOVED] ?: 0
        val untrackedCount = counts[LocalFile.Status.UNTRACKED] ?: 0

        if (addedCount == 0) children.remove(added)
        else if (!children.contains(added)) children += added
        added.text = addedCount.toString()
        if (changedCount == 0) children.remove(changed)
        else if (!children.contains(changed)) children += changed
        changed.text = changedCount.toString()
        if (removedCount == 0) children.remove(removed)
        else if (!children.contains(removed)) children += removed
        removed.text = removedCount.toString()
        if (untrackedCount == 0) children.remove(untracked)
        else if (!children.contains(untracked)) children += untracked
        untracked.text = untrackedCount.toString()
    }

}
