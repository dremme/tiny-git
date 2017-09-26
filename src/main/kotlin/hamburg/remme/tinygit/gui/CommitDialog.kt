package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback

class CommitDialog(repository: LocalRepository, window: Window) : Dialog<Unit>() {

    init {
        title = "New Commit"
        isResizable = true
        initModality(Modality.WINDOW_MODAL)
        initOwner(window)

        val ok = ButtonType("Commit", ButtonBar.ButtonData.OK_DONE)
        val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        val files = FileStatusView()
        files.items.addAll(LocalGit.status(repository).staged)
        files.prefHeight = 250.0
        GridPane.setHgrow(files, Priority.ALWAYS)
        GridPane.setVgrow(files, Priority.ALWAYS)

        val message = textArea(placeholder = "Enter commit message")
        message.textProperty().addListener { _, _, new ->
            dialogPane.lookupButton(ok).isDisable = new.isBlank() || files.items.isEmpty()
        }
        message.prefHeight = 100.0
        GridPane.setHgrow(message, Priority.ALWAYS)

        val amend = CheckBox("Amend last commit.")

        val content = GridPane()
        content.styleClass += "commit-view"
        content.add(files, 0, 0)
        content.add(message, 0, 1)
        content.add(amend, 0, 2)

        resultConverter = Callback {
            if (it != null && it.buttonData.isDefaultButton) {
                if (amend.isSelected) LocalGit.commitAmend(repository, message.text)
                else LocalGit.commit(repository, message.text)
//                State.fireRefresh()
            }
        }
        dialogPane.content = content
        dialogPane.buttonTypes.addAll(cancel, ok)
        dialogPane.lookupButton(ok).isDisable = true
    }

}
