package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import org.kordamp.ikonli.fontawesome.FontAwesome

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
                LocalFile.Status.CONFLICT -> icon(FontAwesome.WARNING, "#d9534f")
                LocalFile.Status.ADDED -> icon(FontAwesome.PLUS, "#5cb85c")
                LocalFile.Status.MODIFIED -> icon(FontAwesome.PENCIL, "#f0ad4e")
                LocalFile.Status.CHANGED -> icon(FontAwesome.PENCIL, "#f0ad4e")
                LocalFile.Status.REMOVED -> icon(FontAwesome.MINUS, "#d9534f")
                LocalFile.Status.MISSING -> icon(FontAwesome.MINUS, "#999")
                LocalFile.Status.UNTRACKED -> icon(FontAwesome.QUESTION, "#5bc0de")
                else -> null
            }
        }

    }

}
