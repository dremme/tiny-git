package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.Git
import hamburg.remme.tinygit.git.gitStatus
import hamburg.remme.tinygit.gui.FileDiffView
import hamburg.remme.tinygit.gui.FileStatusView
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.checkBox
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.layout.Priority
import javafx.stage.Window
import org.eclipse.jgit.api.errors.WrongRepositoryStateException

class CommitDialog(repository: Repository, window: Window)
    : Dialog<Unit>(window, if (State.isMerging.get()) "Merge Commit" else "New Commit", true) {

    init {
        val fileDiff = FileDiffView()
        val files = FileStatusView()
        files.prefWidth = 400.0
        files.prefHeight = 500.0
        files.selectionModel.selectedItemProperty().addListener { _, _, it -> it?.let { fileDiff.update(repository, it) } }

        val message = textArea {
            promptText = "Enter commit message"
            prefHeight = 100.0
            textProperty().bindBidirectional(State.commitMessage)
            Platform.runLater { requestFocus() }
        }
        if (State.isMerging.get() && message.text.isNullOrBlank()) message.text = Git.mergeMessage(repository)

        val amend = checkBox {
            text = "Amend last commit."
            selectedProperty().addListener { _, _, it ->
                if (it && message.text.isNullOrBlank()) message.text = Git.headMessage(repository)
            }
        }

        +DialogButton(DialogButton.ok("Commit"),
                message.textProperty().isEmpty.or(Bindings.isEmpty(files.items)))
        +DialogButton(DialogButton.CANCEL)

        focusAction = {
            val selected = files.selectionModel.selectedItem
            files.items.setAll(gitStatus(repository).staged)
            files.selectionModel.select(files.items.indexOf(selected))
            files.selectionModel.selectedItem ?: files.selectionModel.selectFirst()
        }
        okAction = {
            try {
                if (amend.isSelected) Git.commitAmend(repository, message.text)
                else Git.commit(repository, message.text)
            } catch (ex: WrongRepositoryStateException) {
                errorAlert(dialogWindow, "Cannot Commit", "Cannot commit because there are unmerged changes.")
                throw ex
            }

            State.commitMessage.set("")
            State.fireRefresh(this)
        }
        content = vbox {
            addClass("commit-view")

            +splitPane {
                vgrow(Priority.ALWAYS)
                +files
                +fileDiff
            }
            +message
            if (!State.isMerging.get()) +amend
        }
    }

}
