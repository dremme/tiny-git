package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView

inline fun <T> tree(block: TreeViewBuilder<T>.() -> Unit): TreeView<T> {
    val tree = TreeViewBuilder<T>()
    block(tree)
    return tree
}

class TreeViewBuilder<T> : TreeView<T>() {

    init {
        isShowRoot = false
        root = TreeItem()
    }

    operator fun TreeItem<T>.unaryPlus() {
        root.children += this
    }

}
