package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.RebaseException
import hamburg.remme.tinygit.git.UnmergedException
import hamburg.remme.tinygit.git.gitIsRebasing
import hamburg.remme.tinygit.git.gitRebase
import hamburg.remme.tinygit.git.gitRebaseAbort
import hamburg.remme.tinygit.git.gitRebaseContinue
import hamburg.remme.tinygit.git.gitRebaseStatus
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task

class RebaseService : Refreshable {

    val isRebasing = SimpleBooleanProperty()
    val rebaseNext = SimpleIntegerProperty()
    val rebaseLast = SimpleIntegerProperty()
    private lateinit var repository: Repository

    fun rebase(branch: Branch, errorHandler: (String) -> Unit) {
        TinyGit.execute("Rebasing...", object : Task<Unit>() {
            override fun call() = gitRebase(repository, branch)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is RebaseException -> errorHandler.invoke(exception.message!!)
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun `continue`(unresolvedHandler: () -> Unit) {
        TinyGit.execute("Rebasing...", object : Task<Unit>() {
            override fun call() = gitRebaseContinue(repository)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is UnmergedException -> unresolvedHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun abort() {
        TinyGit.execute("Aborting...", object : Task<Unit>() {
            override fun call() = gitRebaseAbort(repository)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        rebaseNext.set(0)
        rebaseLast.set(0)
        isRebasing.set(false)
    }

    private fun update(repository: Repository) {
        this.repository = repository
        val (next, last) = gitRebaseStatus(repository)
        rebaseNext.set(next)
        rebaseLast.set(last)
        isRebasing.set(gitIsRebasing(repository))
    }

}
