package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchNameInvalidException
import hamburg.remme.tinygit.git.CheckoutException
import hamburg.remme.tinygit.git.gitBranch
import hamburg.remme.tinygit.git.gitBranchDelete
import hamburg.remme.tinygit.git.gitBranchList
import hamburg.remme.tinygit.git.gitBranchMove
import hamburg.remme.tinygit.git.gitCheckout
import hamburg.remme.tinygit.git.gitCheckoutRemote
import hamburg.remme.tinygit.git.gitHead
import hamburg.remme.tinygit.git.gitResetHard
import hamburg.remme.tinygit.git.gitSquash
import hamburg.remme.tinygit.observableList
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task

class BranchService : Refreshable {

    val head = SimpleStringProperty("")
    val branches = observableList<Branch>()
    val branchesSize = Bindings.size(branches)!!
    private lateinit var repository: Repository
    private var task: Task<*>? = null

    fun checkoutLocal(branch: String, errorHandler: () -> Unit) {
        if (branch != head.get()) {
            TinyGit.execute("Switching branches...", object : Task<Unit>() {
                override fun call() = gitCheckout(repository, branch)

                override fun succeeded() = TinyGit.fireEvent()

                override fun failed() {
                    when (exception) {
                        is CheckoutException -> errorHandler.invoke()
                        else -> exception.printStackTrace()
                    }
                }
            })
        }
    }

    fun checkoutRemote(branch: String, errorHandler: () -> Unit) {
        TinyGit.execute("Getting remote branch...", object : Task<Unit>() {
            override fun call() = gitCheckoutRemote(repository, branch)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is CheckoutException -> checkoutLocal(branch.substringAfter('/'), errorHandler)
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun rename(branch: String, newName: String) {
        gitBranchMove(repository, branch, newName)
        TinyGit.fireEvent()
    }

    fun branch(name: String, branchExistsHandler: () -> Unit, nameInvalidHandler: () -> Unit) {
        TinyGit.execute("Branching...", object : Task<Unit>() {
            override fun call() = gitBranch(repository, name)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is BranchAlreadyExistsException -> branchExistsHandler.invoke()
                    is BranchNameInvalidException -> nameInvalidHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun delete(branch: String, force: Boolean) {
        gitBranchDelete(repository, branch, force)
        TinyGit.fireEvent()
    }

    fun autoReset() {
        TinyGit.execute("Resetting branch...", object : Task<Unit>() {
            override fun call() = gitResetHard(repository, head.get())

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun autoSquash(baseId: String, message: String) {
        TinyGit.execute("Squashing branch...", object : Task<Unit>() {
            override fun call() = gitSquash(repository, baseId, message)

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
        head.set("")
        branches.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        task?.cancel()
        task = object : Task<List<Branch>>() {
            private lateinit var value1: String

            override fun call(): List<Branch> {
                value1 = gitHead(repository)
                return gitBranchList(repository)
            }

            override fun succeeded() {
                head.set(value1)
                branches.setAll(value)
            }
        }.also { TinyGit.execute(it) }
    }

}
