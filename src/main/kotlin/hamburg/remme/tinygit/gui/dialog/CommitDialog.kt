package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.FileDiffView
import hamburg.remme.tinygit.gui.FileStatusView
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.checkBox
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.layout.Priority
import javafx.stage.Window

class CommitDialog(repository: LocalRepository, window: Window) : Dialog(window, "New Commit", true) {

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
            files.items.setAll(Git.status(repository).staged)
            files.selectionModel.select(files.items.indexOf(selected))
            files.selectionModel.selectedItem ?: files.selectionModel.selectFirst()
        }
        okAction = {
            if (amend.isSelected) Git.commitAmend(repository, message.text)
            else Git.commit(repository, message.text)

            State.commitMessage.set("")
            State.fireRefresh()
        }
        content = vbox {
            addClass("commit-view")

            +splitPane {
                vgrow(Priority.ALWAYS)
                +files
                +fileDiff
            }
            +message
            +amend
        }
    }

}
