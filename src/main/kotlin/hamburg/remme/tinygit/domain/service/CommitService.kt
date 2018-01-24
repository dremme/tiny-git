package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitCommit
import hamburg.remme.tinygit.git.gitCommitAmend
import javafx.concurrent.Task

class CommitService(private val service: WorkingCopyService) : Refreshable {

    private lateinit var repository: Repository

    fun commit(message: String, amend: Boolean, errorHandler: () -> Unit) {
        TinyGit.execute("Committing...", task = object : Task<Unit>() {
            override fun call() {
                if (amend) gitCommitAmend(repository, message)
                else gitCommit(repository, message)
            }

            override fun succeeded() {
                service.message.set("")
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
