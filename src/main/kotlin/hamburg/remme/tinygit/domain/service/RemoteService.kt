package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.BranchBehindException
import hamburg.remme.tinygit.git.MergeConflictException
import hamburg.remme.tinygit.git.PullException
import hamburg.remme.tinygit.git.TimeoutException
import hamburg.remme.tinygit.git.gitFetchPrune
import hamburg.remme.tinygit.git.gitPull
import hamburg.remme.tinygit.git.gitPush
import javafx.concurrent.Task

class RemoteService(private val repositoryService: RepositoryService,
                    private val credentialService: CredentialService) : Refreshable {

    private lateinit var repository: Repository

    fun push(force: Boolean, behindHandler: () -> Unit, timeoutHandler: () -> Unit) {
        credentialService.applyCredentials(repositoryService.remote.get())
        TinyGit.execute(I18N["remote.push"], object : Task<Unit>() {
            override fun call() = gitPush(repository, force)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is BranchBehindException -> behindHandler()
                    is TimeoutException -> timeoutHandler()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun fetch() {
        credentialService.applyCredentials(repositoryService.remote.get())
        TinyGit.execute(I18N["remote.fetch"], object : Task<Unit>() {
            override fun call() = gitFetchPrune(repository)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun pull(errorHandler: (String) -> Unit, conflictHandler: () -> Unit, timeoutHandler: () -> Unit) {
        credentialService.applyCredentials(repositoryService.remote.get())
        TinyGit.execute(I18N["remote.pull"], object : Task<Unit>() {
            override fun call() = gitPull(repository)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is MergeConflictException -> {
                        TinyGit.fireEvent()
                        conflictHandler()
                    }
                    is PullException -> errorHandler(exception.message!!)
                    is TimeoutException -> timeoutHandler()
                    else -> exception.printStackTrace()
                }
            }
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
