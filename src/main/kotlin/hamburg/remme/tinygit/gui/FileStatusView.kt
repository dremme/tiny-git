package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode

class FileStatusView : ListView<LocalFile> {

    companion object {

        fun conflictIcon() = FontAwesome.exclamation()
        fun addedIcon() = FontAwesome.plus()
        fun copiedIcon() = FontAwesome.plus()
        fun renamedIcon() = FontAwesome.share()
        fun modifiedIcon() = FontAwesome.pencil()
        fun changedIcon() = FontAwesome.pencil()
        fun removedIcon() = FontAwesome.minus()
        fun missingIcon() = FontAwesome.minus()
        fun untrackedIcon() = FontAwesome.question()

    }

    constructor() : super() {
        selectionModel.selectionMode = SelectionMode.SINGLE
    }

    constructor(list: ObservableList<LocalFile>, selectionMode: SelectionMode) : super(list) {
        selectionModel.selectionMode = selectionMode
    }

    init {
        addClass("file-status-view")
        setCellFactory { LocalFileListCell() }
    }

    private class LocalFileListCell : ListCell<LocalFile>() {

        override fun updateItem(item: LocalFile?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.path
            graphic = when (item?.status) {
                LocalFile.Status.CONFLICT -> conflictIcon().addClass("status-conflict")
                LocalFile.Status.ADDED -> addedIcon().addClass("status-added")
                LocalFile.Status.COPIED -> copiedIcon().addClass("status-copied")
                LocalFile.Status.RENAMED -> renamedIcon().addClass("status-renamed")
                LocalFile.Status.MODIFIED -> modifiedIcon().addClass("status-modified")
                LocalFile.Status.CHANGED -> changedIcon().addClass("status-changed")
                LocalFile.Status.REMOVED -> removedIcon().addClass("status-removed")
                LocalFile.Status.MISSING -> missingIcon().addClass("status-missing")
                LocalFile.Status.UNTRACKED -> untrackedIcon().addClass("status-untracked")
                else -> null
            }
        }

    }

}
