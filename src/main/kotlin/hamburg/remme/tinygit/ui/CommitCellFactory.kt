package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.system.git.CommitProperty
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

/**
 * Cell factory for creating cells that display commits.
 */
class CommitCellFactory : Callback<ListView<Map<CommitProperty, Any>>, ListCell<Map<CommitProperty, Any>>> {

    override fun call(param: ListView<Map<CommitProperty, Any>>): ListCell<Map<CommitProperty, Any>> {
        return object : ListCell<Map<CommitProperty, Any>>() {

            override fun updateItem(item: Map<CommitProperty, Any>?, empty: Boolean) {
                super.updateItem(item, empty)
                item?.let { text = it[CommitProperty.H].toString() }
            }

        }
    }

}
