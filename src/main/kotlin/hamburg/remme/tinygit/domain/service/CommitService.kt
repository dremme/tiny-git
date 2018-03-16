package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitCommit
import hamburg.remme.tinygit.git.gitCommitAmend
import hamburg.remme.tinygit.git.gitHeadMessage
import hamburg.remme.tinygit.git.gitMergeMessage
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task

class CommitService : Refreshable {

    val message = SimpleStringProperty("")
    private lateinit var repository: Repository

    fun setMergeMessage() {
        if (message.get().isBlank()) message.set(gitMergeMessage(repository))
    }

    fun setHeadMessage() {
        if (message.get().isBlank()) message.set(gitHeadMessage(repository))
    }

    fun commit(message: String, amend: Boolean, errorHandler: () -> Unit) {
        TinyGit.execute(I18N["commit.committing"], object : Task<Unit>() {
            override fun call() {
                if (amend) gitCommitAmend(repository, message)
                else gitCommit(repository, message)
            }

            override fun succeeded() {
                this@CommitService.message.set("")
                TinyGit.fireEvent()
            }

            override fun failed() = errorHandler.invoke()
        })
    }

    override fun onRefresh(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryChanged(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryDeselected() {
    }

}
