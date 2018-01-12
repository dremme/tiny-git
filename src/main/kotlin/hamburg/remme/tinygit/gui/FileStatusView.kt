package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.Icons
import javafx.collections.ObservableList
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode

class FileStatusView : ListView<GitFile> {

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

    constructor(list: ObservableList<GitFile>, selectionMode: SelectionMode) : super(list) {
        selectionModel.selectionMode = selectionMode
    }

    init {
        addClass("file-status-view")
        setCellFactory { LocalFileListCell() }
    }

    private class LocalFileListCell : ListCell<GitFile>() {

        override fun updateItem(item: GitFile?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.path
            graphic = when {
                item?.status == GitFile.Status.CONFLICT -> conflictIcon().addClass("status-conflict")
                item?.status == GitFile.Status.ADDED && !item.cached -> untrackedIcon().addClass("status-untracked")
                item?.status == GitFile.Status.ADDED -> addedIcon().addClass("status-added")
                item?.status == GitFile.Status.COPIED -> copiedIcon().addClass("status-copied")
                item?.status == GitFile.Status.RENAMED -> renamedIcon().addClass("status-renamed")
                item?.status == GitFile.Status.MODIFIED -> modifiedIcon().addClass("status-modified")
                item?.status == GitFile.Status.REMOVED && !item.cached -> missingIcon().addClass("status-missing")
                item?.status == GitFile.Status.REMOVED -> removedIcon().addClass("status-removed")
                else -> null
            }
        }

    }

}
