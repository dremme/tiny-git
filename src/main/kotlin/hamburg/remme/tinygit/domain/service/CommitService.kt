package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitCommit
import hamburg.remme.tinygit.git.gitCommitAmend
import javafx.concurrent.Task

object CommitService : Refreshable {

    private lateinit var repository: Repository

    fun commit(message: String, amend: Boolean, errorHandler: () -> Unit) {
        State.startProcess("Committing...", task = object : Task<Unit>() {
            override fun call() {
                if (amend) gitCommitAmend(repository, message)
                else gitCommit(repository, message)
            }

            override fun succeeded() {
                WorkingCopyService.message.set("")
                State.fireRefresh()
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
