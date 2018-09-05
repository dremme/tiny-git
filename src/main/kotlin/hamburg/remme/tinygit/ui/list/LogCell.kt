package hamburg.remme.tinygit.ui.list

import hamburg.remme.tinygit.system.git.Commit
import javafx.fxml.FXML
import javafx.scene.control.Label
import java.util.ResourceBundle

/**
 * List cell displaying information about a commit, like a log entry.
 */
class LogCell(resources: ResourceBundle) : FXMLListCell<Commit>("/fxml/list/log_cell.fxml", resources) {

    @FXML private lateinit var shortIdLabel: Label
    @FXML private lateinit var timeLabel: Label
    @FXML private lateinit var messageLabel: Label
    @FXML private lateinit var nameLabel: Label

    override fun updateItem(commit: Commit?, empty: Boolean) {
        super.updateItem(commit, empty)
        if (empty) {
            shortIdLabel.text = ""
            timeLabel.text = ""
            messageLabel.text = ""
            nameLabel.text = ""
        } else {
            shortIdLabel.text = commit!!.shortId
            timeLabel.text = commit.committerTime.toString()
            messageLabel.text = "---" // TODO
            nameLabel.text = commit.committerName
        }
    }

}
