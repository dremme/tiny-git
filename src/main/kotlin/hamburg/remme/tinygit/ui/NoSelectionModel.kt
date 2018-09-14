package hamburg.remme.tinygit.ui

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.MultipleSelectionModel

/**
 * A special selection model where nothing can be selected. This is necessary for lists that do not have selectable
 * items, but still need to receive mouse events.
 */
class NoSelectionModel<T> : MultipleSelectionModel<T>() {

    private val selectedIndices: ObservableList<Int> = FXCollections.emptyObservableList()
    private val selectedItems: ObservableList<T> = FXCollections.emptyObservableList()

    override fun getSelectedIndices(): ObservableList<Int> = selectedIndices

    override fun getSelectedItems(): ObservableList<T> = selectedItems

    override fun clearAndSelect(index: Int): Unit = Unit

    override fun clearSelection(index: Int): Unit = Unit

    override fun clearSelection(): Unit = Unit

    override fun isEmpty(): Boolean = true

    override fun isSelected(index: Int): Boolean = false

    override fun select(index: Int): Unit = Unit

    override fun select(obj: T): Unit = Unit

    override fun selectAll(): Unit = Unit

    override fun selectFirst(): Unit = Unit

    override fun selectIndices(index: Int, vararg indices: Int): Unit = Unit

    override fun selectLast(): Unit = Unit

    override fun selectNext(): Unit = Unit

    override fun selectPrevious(): Unit = Unit

}
