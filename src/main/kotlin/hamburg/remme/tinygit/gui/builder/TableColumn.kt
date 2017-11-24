package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.TableColumn

inline fun <S, T> column(block: TableColumn<S, T>.() -> Unit): TableColumn<S, T> {
    val column = TableColumn<S, T>()
    block.invoke(column)
    return column
}
