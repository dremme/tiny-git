package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.UnmergedException
import hamburg.remme.tinygit.git.gitIsRebasing
import hamburg.remme.tinygit.git.gitRebase
import hamburg.remme.tinygit.git.gitRebaseAbort
import hamburg.remme.tinygit.git.gitRebaseContinue
import hamburg.remme.tinygit.git.gitRebaseStatus
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task

object RebaseService : Refreshable {

    val isRebasing = SimpleBooleanProperty()
    val rebaseNext = SimpleIntegerProperty()
    val rebaseLast = SimpleIntegerProperty()
    private lateinit var repository: Repository

    fun rebase(rebaseBase: String) {
        State.startProcess("Rebasing...", object : Task<Unit>() {
            override fun call() = gitRebase(repository, rebaseBase)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun `continue`(unresolvedHandler: () -> Unit) {
        State.startProcess("Rebasing...", object : Task<Unit>() {
            override fun call() = gitRebaseContinue(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is UnmergedException -> unresolvedHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun abort() {
        State.startProcess("Aborting...", object : Task<Unit>() {
            override fun call() = gitRebaseAbort(repository)

            override fun succeeded() = State.fireRefresh()

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
