package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.util.Callback

inline fun <T> listCell(crossinline block: ListCell<T>.(T?) -> Unit): ListCell<T> {
    return object : ListCell<T>() {
        override fun updateItem(item: T?, empty: Boolean) {
            super.updateItem(item, empty)
            block(item)
        }
    }
}

inline fun <T> listCellFactory(crossinline block: ListCell<T>.(T?) -> Unit): Callback<ListView<T>, ListCell<T>> {
    return Callback {
        object : ListCell<T>() {
            override fun updateItem(item: T?, empty: Boolean) {
                super.updateItem(item, empty)
                block(item)
            }
        }
    }
}

inline fun <T> tree(block: TreeViewBuilder<T>.() -> Unit): TreeView<T> {
    val tree = TreeViewBuilder<T>()
    block(tree)
    return tree
}

class TreeViewBuilder<T> : TreeView<T>() {

    val selectedValue get() = selectionModel.selectedItem?.value

    init {
        isShowRoot = false
        root = TreeItem()
    }

    operator fun TreeItem<T>.unaryPlus() {
        root.children += this
    }

}
