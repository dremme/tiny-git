package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchNameInvalidException
import hamburg.remme.tinygit.git.gitBranch
import hamburg.remme.tinygit.git.gitBranchList
import hamburg.remme.tinygit.git.gitHead
import hamburg.remme.tinygit.git.gitResetHard
import hamburg.remme.tinygit.git.gitSquash
import hamburg.remme.tinygit.observableList
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task

object BranchService : Refreshable {

    val head = SimpleStringProperty("")
    val branches = observableList<Branch>()
    val branchesSize = Bindings.size(branches)!!
    private lateinit var repository: Repository

    fun branch(name: String, branchExistsHandler: () -> Unit, nameInvalidHandler: () -> Unit) {
        State.startProcess("Branching...", object : Task<Unit>() {
            override fun call() = gitBranch(repository, name)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is BranchAlreadyExistsException -> branchExistsHandler.invoke()
                    is BranchNameInvalidException -> nameInvalidHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun autoReset() {
        State.startProcess("Resetting branch...", object : Task<Unit>() {
            override fun call() = gitResetHard(repository, head.get())

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun autoSquash(baseId: String, message: String) {
        State.startProcess("Squashing branch...", object : Task<Unit>() {
            override fun call() = gitSquash(repository, baseId, message)

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
        head.set("")
        branches.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        head.set(gitHead(repository))
        branches.setAll(gitBranchList(repository))
    }

}
