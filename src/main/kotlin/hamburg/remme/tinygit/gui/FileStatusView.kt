package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.Icons
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.util.Callback

const val CONFLICT_STYLE_CLASS = "status-conflict"
const val UNTRACKED_STYLE_CLASS = "status-untracked"
const val ADDED_STYLE_CLASS = "status-added"
const val COPIED_STYLE_CLASS = "status-copied"
const val RENAMED_STYLE_CLASS = "status-renamed"
const val MODIFIED_STYLE_CLASS = "status-modified"
const val MISSING_STYLE_CLASS = "status-missing"
const val REMOVED_STYLE_CLASS = "status-removed"
private const val DEFAULT_STYLE_CLASS = "file-status-view"

/**
 * File list for the working copy or a commit, with status icons.
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 *   ┃ * .gitignore               ┃
 *   ┃ * src/kotlin/Another.kt    ┃
 *   ┃ * src/kotlin/MyClass.kt    ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @see ListView
 */
class FileStatusView(
    list: ObservableList<File>,
    selectionMode: SelectionMode = SelectionMode.SINGLE,
) : ListView<File>(list) {
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
        addClass(DEFAULT_STYLE_CLASS)
        cellFactory = Callback { LocalFileListCell() }
        selectionModel.selectionMode = selectionMode
    }

    /** Reuses icon nodes so updateItem does not recreate graphics (avoids selection flicker). */
    private class LocalFileListCell : ListCell<File>() {
        private val icons =
            mapOf(
                File.Status.CONFLICT to conflictIcon().addClass(CONFLICT_STYLE_CLASS),
                File.Status.COPIED to copiedIcon().addClass(COPIED_STYLE_CLASS),
                File.Status.RENAMED to renamedIcon().addClass(RENAMED_STYLE_CLASS),
                File.Status.MODIFIED to modifiedIcon().addClass(MODIFIED_STYLE_CLASS),
            )
        private val added = addedIcon().addClass(ADDED_STYLE_CLASS)
        private val untracked = untrackedIcon().addClass(UNTRACKED_STYLE_CLASS)
        private val removed = removedIcon().addClass(REMOVED_STYLE_CLASS)
        private val missing = missingIcon().addClass(MISSING_STYLE_CLASS)

        override fun updateItem(
            item: File?,
            empty: Boolean,
        ) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null
                graphic = null
                return
            }
            text = item.path
            val next = iconFor(item)
            if (graphic !== next) graphic = next
        }

        private fun iconFor(file: File): Node =
            when (file.status) {
                File.Status.ADDED -> if (file.isCached) added else untracked
                File.Status.REMOVED -> if (file.isCached) removed else missing
                else -> icons.getValue(file.status)
            }
    }
}
