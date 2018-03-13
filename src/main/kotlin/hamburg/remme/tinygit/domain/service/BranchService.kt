package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Head
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchNameInvalidException
import hamburg.remme.tinygit.git.BranchUnpushedException
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
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task

class BranchService : Refreshable {

    val head = SimpleObjectProperty<Head>(Head.EMPTY)
    val branches = observableList<Branch>()
    val branchesSize = Bindings.size(branches)!!
    private lateinit var repository: Repository
    private var task: Task<*>? = null

    fun isHead(branch: Branch) = head.get().name == branch.name

    fun isDetached(branch: Branch) = branch.name == "HEAD"

    fun checkoutCommit(commit: Commit, errorHandler: () -> Unit) {
        TinyGit.execute("Checking out...", object : Task<Unit>() {
            override fun call() = gitCheckout(repository, commit)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is CheckoutException -> errorHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun checkoutLocal(branch: Branch, errorHandler: () -> Unit) {
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

    fun checkoutRemote(branch: Branch, errorHandler: () -> Unit) {
        TinyGit.execute("Getting remote branch...", object : Task<Unit>() {
            override fun call() = gitCheckoutRemote(repository, branch)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is CheckoutException -> checkoutLocal(Branch("", branch.name.substringAfter('/'), false), errorHandler)
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun rename(branch: Branch, newName: String, branchExistsHandler: () -> Unit) {
        try {
            gitBranchMove(repository, branch, newName)
            TinyGit.fireEvent()
        } catch (ex: BranchAlreadyExistsException) {
            branchExistsHandler.invoke()
        }
    }

    fun delete(branch: Branch, force: Boolean, branchUnpushedHandler: () -> Unit = {}) {
        try {
            gitBranchDelete(repository, branch, force)
            TinyGit.fireEvent()
        } catch (ex: BranchUnpushedException) {
            branchUnpushedHandler.invoke()
        }
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

    fun reset(commit: Commit) {
        TinyGit.execute("Resetting...", object : Task<Unit>() {
            override fun call() = gitResetHard(repository, commit)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
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
        head.set(Head.EMPTY)
        branches.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        task?.cancel()
        task = object : Task<Unit>() {
            private lateinit var head: Head
            private lateinit var branches: List<Branch>

            override fun call() {
                head = gitHead(repository)
                branches = gitBranchList(repository)
            }

            override fun succeeded() {
                this@BranchService.head.set(head)
                this@BranchService.branches.setAll(branches)
            }
        }.also { TinyGit.execute(it) }
    }

}
