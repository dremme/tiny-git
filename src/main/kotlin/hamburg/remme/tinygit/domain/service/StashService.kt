package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitStash
import hamburg.remme.tinygit.git.gitStashList
import hamburg.remme.tinygit.git.gitStashPop
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task

object StashService : Refreshable {

    val stashSize = SimpleIntegerProperty()
    private lateinit var repository: Repository

    fun stash() {
        State.startProcess("Stashing files...", object : Task<Unit>() {
            override fun call() = gitStash(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun pop(cannotPopHandler: () -> Unit) {
        State.startProcess("Applying stash...", object : Task<Unit>() {
            override fun call() = gitStashPop(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is RuntimeException -> { // TODO
                        State.fireRefresh()
                        cannotPopHandler.invoke()
                    }
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        stashSize.set(0)
    }

    private fun update(repository: Repository) {
        this.repository = repository
        stashSize.set(gitStashList(repository).size)
    }

}
