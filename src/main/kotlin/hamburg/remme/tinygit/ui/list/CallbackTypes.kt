package hamburg.remme.tinygit.ui.list

import hamburg.remme.tinygit.system.git.Commit
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

/**
 * Callback for commit list cells, basically log entries.
 */
internal typealias LogCellCallback = Callback<ListView<Commit>, ListCell<Commit>>
