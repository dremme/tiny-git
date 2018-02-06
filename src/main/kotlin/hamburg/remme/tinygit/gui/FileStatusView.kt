package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.Icons
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode

class FileStatusView(list: ObservableList<File>, selectionMode: SelectionMode = SelectionMode.SINGLE) : ListView<File>(list) {

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

    init {
        addClass("file-status-view")
        setCellFactory { LocalFileListCell() }
        selectionModel.selectionMode = selectionMode
    }

    private inner class LocalFileListCell : ListCell<File>() {

        override fun updateItem(item: File?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.path
            graphic = when {
                item?.status == File.Status.CONFLICT -> conflictIcon().addClass("status-conflict")
                item?.status == File.Status.ADDED && !item.isCached -> untrackedIcon().addClass("status-untracked")
                item?.status == File.Status.ADDED -> addedIcon().addClass("status-added")
                item?.status == File.Status.COPIED -> copiedIcon().addClass("status-copied")
                item?.status == File.Status.RENAMED -> renamedIcon().addClass("status-renamed")
                item?.status == File.Status.MODIFIED -> modifiedIcon().addClass("status-modified")
                item?.status == File.Status.REMOVED && !item.isCached -> missingIcon().addClass("status-missing")
                item?.status == File.Status.REMOVED -> removedIcon().addClass("status-removed")
                else -> null
            }
        }

    }

}
