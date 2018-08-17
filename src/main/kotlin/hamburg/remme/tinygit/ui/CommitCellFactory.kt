package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.system.git.Commit
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

/**
 * Cell factory for creating cells that display commits.
 */
class CommitCellFactory : Callback<ListView<Commit>, ListCell<Commit>> {

    override fun call(param: ListView<Commit>): ListCell<Commit> {
        return object : ListCell<Commit>() {

            override fun updateItem(item: Commit?, empty: Boolean) {
                super.updateItem(item, empty)
                text = item?.id
            }

        }
    }

}
