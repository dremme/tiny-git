package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry
import hamburg.remme.tinygit.git.StashConflictException
import hamburg.remme.tinygit.git.gitStash
import hamburg.remme.tinygit.git.gitStashApply
import hamburg.remme.tinygit.git.gitStashDrop
import hamburg.remme.tinygit.git.gitStashList
import hamburg.remme.tinygit.git.gitStashPop
import hamburg.remme.tinygit.observableList
import javafx.beans.binding.Bindings
import javafx.concurrent.Task

class StashService : Refreshable {

    val stashEntries = observableList<StashEntry>()
    val stashSize = Bindings.size(stashEntries)!!
    private lateinit var repository: Repository
    private var task: Task<*>? = null

    fun create() {
        TinyGit.execute("Stashing files...", object : Task<Unit>() {
            override fun call() = gitStash(repository)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun apply(stashEntry: StashEntry, conflictHandler: () -> Unit) {
        TinyGit.execute("Applying stash entry...", object : Task<Unit>() {
            override fun call() = gitStashApply(repository, stashEntry)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is StashConflictException -> {
                        TinyGit.fireEvent()
                        conflictHandler.invoke()
                    }
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun pop(conflictHandler: () -> Unit) {
        TinyGit.execute("Applying stash entry...", object : Task<Unit>() {
            override fun call() = gitStashPop(repository)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is StashConflictException -> {
                        TinyGit.fireEvent()
                        conflictHandler.invoke()
                    }
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun drop(stashEntry: StashEntry) {
        TinyGit.execute("Dropping stash entry...", object : Task<Unit>() {
            override fun call() = gitStashDrop(repository, stashEntry)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        onRepositoryDeselected()
        update(repository)
    }

    override fun onRepositoryDeselected() {
        task?.cancel()
        stashEntries.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        task?.cancel()
        task = object : Task<List<StashEntry>>() {
            override fun call() = gitStashList(repository)

            override fun succeeded() {
                stashEntries.setAll(value)
            }
        }.also { TinyGit.execute(it) }
    }

}
