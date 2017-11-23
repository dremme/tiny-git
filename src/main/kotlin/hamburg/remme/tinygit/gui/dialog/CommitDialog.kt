package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.FileDiffView
import hamburg.remme.tinygit.gui.FileStatusView
import hamburg.remme.tinygit.gui._textArea
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Window

class CommitDialog(repository: LocalRepository, window: Window) : Dialog(window, "New Commit", true) {

    init {
        val ok = ButtonType("Commit", ButtonBar.ButtonData.OK_DONE)
        val cancel = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

        val fileDiff = FileDiffView()

        val files = FileStatusView()
        files.prefWidth = 400.0
        files.prefHeight = 500.0
        files.selectionModel.selectedItemProperty().addListener { _, _, it -> it?.let { fileDiff.update(repository, it) } }

        val message = _textArea(placeholder = "Enter commit message")
        message.prefHeight = 100.0
        message.textProperty().bindBidirectional(State.commitMessage)
        Platform.runLater { message.requestFocus() }

        val amend = CheckBox("Amend last commit.")
        amend.selectedProperty().addListener { _, _, it ->
            if (it && message.text.isNullOrBlank()) message.text = Git.headMessage(repository)
        }

        val content = VBox(
                SplitPane(files, fileDiff).also { VBox.setVgrow(it, Priority.ALWAYS) },
                message, amend)
                .addClass("commit-view")

        okAction = {
            if (amend.isSelected) Git.commitAmend(repository, message.text)
            else Git.commit(repository, message.text)

            message.textProperty().unbindBidirectional(State.commitMessage)
            State.commitMessage.set("")

            State.fireRefresh()
        }
        setContent(content)
        setButton(cancel, ok)
        setButtonBinding(ok, message.textProperty().isEmpty.or(Bindings.isEmpty(files.items)))

        focusAction = {
            val selected = files.selectionModel.selectedItem
            files.items.setAll(Git.status(repository).staged)
            files.selectionModel.select(files.items.indexOf(selected))
            files.selectionModel.selectedItem ?: files.selectionModel.selectFirst()
        }
    }

}
