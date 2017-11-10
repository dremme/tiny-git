package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.gui.FileDiffView
import hamburg.remme.tinygit.gui.FileStatusView
import hamburg.remme.tinygit.gui.textArea
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
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

        val fileDiff = FileDiffView()

        val files = FileStatusView()
        files.prefWidth = 400.0
        files.prefHeight = 500.0
        files.items.addAll(LocalGit.status(repository).staged)
        files.selectionModel.selectedItemProperty().addListener { _, _, it ->
            fileDiff.update(repository, it)
        }
        files.selectionModel.selectFirst()

        val message = textArea(placeholder = "Enter commit message")
        message.prefHeight = 100.0
        message.textProperty().bindBidirectional(State.commitMessage)

        // TODO: get previous message on amend
        val amend = CheckBox("Amend last commit.")

        val content = VBox(
                SplitPane(files, fileDiff).also { VBox.setVgrow(it, Priority.ALWAYS) },
                message, amend)
        content.styleClass += "commit-view"

        resultConverter = Callback {
            if (it.buttonData.isDefaultButton) {
                if (amend.isSelected) LocalGit.commitAmend(repository, message.text)
                else LocalGit.commit(repository, message.text)

                State.fireRefresh()
            }
        }
        dialogPane.content = content
        dialogPane.buttonTypes.addAll(cancel, ok)
        dialogPane.lookupButton(ok).disableProperty().bind(message.textProperty().isEmpty.or(Bindings.isEmpty(files.items)))

        Platform.runLater { message.requestFocus() }

        // TODO: needs a refresh listener on focus so evetually removed files are registered
    }

}
