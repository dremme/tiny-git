package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.gui.FontAwesome.EXCLAMATION_TRIANGLE
import hamburg.remme.tinygit.gui.FontAwesome.MINUS
import hamburg.remme.tinygit.gui.FontAwesome.PENCIL
import hamburg.remme.tinygit.gui.FontAwesome.PLUS
import hamburg.remme.tinygit.gui.FontAwesome.QUESTION
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
                LocalFile.Status.CONFLICT -> EXCLAMATION_TRIANGLE("#d9534f")
                LocalFile.Status.ADDED -> PLUS("#5cb85c")
                LocalFile.Status.MODIFIED -> PENCIL("#f0ad4e")
                LocalFile.Status.CHANGED -> PENCIL("#f0ad4e")
                LocalFile.Status.REMOVED -> MINUS("#d9534f")
                LocalFile.Status.MISSING -> MINUS("#999")
                LocalFile.Status.UNTRACKED -> QUESTION("#5bc0de")
                else -> null
            }
        }

    }

}
