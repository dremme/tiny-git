package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode

class FileStatusView : ListView<LocalFile>() {

    init {
        setCellFactory { LocalFileListCell() }
        styleClass += "file-status-view"
        selectionModel.selectionMode = SelectionMode.MULTIPLE
    }

    private class LocalFileListCell : ListCell<LocalFile>() {

        override fun updateItem(item: LocalFile?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.path
            graphic = when (item?.status) {
                LocalFile.Status.ADDED -> FontAwesome.plus()
                LocalFile.Status.MODIFIED -> FontAwesome.pencil()
                LocalFile.Status.CHANGED -> FontAwesome.pencil()
                LocalFile.Status.REMOVED -> FontAwesome.minus()
                LocalFile.Status.UNTRACKED -> FontAwesome.question()
                else -> null
            }
            graphic?.style = "-fx-text-fill:" + when (item?.status) {
                LocalFile.Status.ADDED -> "#5cb85c"
                LocalFile.Status.MODIFIED -> "#f0ad4e"
                LocalFile.Status.CHANGED -> "#f0ad4e"
                LocalFile.Status.REMOVED -> "#d9534f"
                LocalFile.Status.UNTRACKED -> "#5bc0de"
                else -> ""
            }
        }

    }

}
