package hamburg.remme.tinygit.ui.list

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.system.git.Commit
import javafx.fxml.FXML
import javafx.scene.control.Label
import java.util.ResourceBundle

/**
 * List cell displaying information about a commit, like a log entry.
 */
class LogCell(private val settings: Settings, resources: ResourceBundle)
    : FXMLListCell<Commit>("/fxml/list/logcell.fxml", resources) {

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
            commit!!
            shortIdLabel.text = commit.shortId
            timeLabel.text = settings.formatDateTime(commit.committerTime) // TODO: show if author is different
            messageLabel.text = commit.message
            nameLabel.text = commit.committerName // TODO: show if author is different
        }
    }

}
