package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.builder.Icons
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode

class FileStatusView : ListView<LocalFile> {

    companion object {

        fun conflictIcon() = Icons.exclamationTriangle()
        fun addedIcon() = Icons.plus()
        fun copiedIcon() = Icons.plus()
        fun renamedIcon() = Icons.share()
        fun modifiedIcon() = Icons.pencil()
        fun removedIcon() = Icons.minus()
        fun missingIcon() = Icons.minus()
        fun untrackedIcon() = Icons.question()

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
            graphic = when {
                item?.status == LocalFile.Status.CONFLICT -> conflictIcon().addClass("status-conflict")
                item?.status == LocalFile.Status.ADDED && !item.cached -> untrackedIcon().addClass("status-untracked")
                item?.status == LocalFile.Status.ADDED -> addedIcon().addClass("status-added")
                item?.status == LocalFile.Status.COPIED -> copiedIcon().addClass("status-copied")
                item?.status == LocalFile.Status.RENAMED -> renamedIcon().addClass("status-renamed")
                item?.status == LocalFile.Status.MODIFIED -> modifiedIcon().addClass("status-modified")
                item?.status == LocalFile.Status.REMOVED && !item.cached -> missingIcon().addClass("status-missing")
                item?.status == LocalFile.Status.REMOVED -> removedIcon().addClass("status-removed")
                else -> null
            }
        }

    }

}
