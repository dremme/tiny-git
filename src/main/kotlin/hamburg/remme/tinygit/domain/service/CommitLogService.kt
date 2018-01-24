package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.TimeoutException
import hamburg.remme.tinygit.git.gitFetch
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.git.gitUpToDate
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task

class CommitLogService(private val service: RepositoryService) : Refreshable {

    val commits = observableList<Commit>()
    val activeCommit = SimpleObjectProperty<Commit?>()
    lateinit var logExecutor: TaskExecutor
    lateinit var timeoutHandler: () -> Unit
    private lateinit var repository: Repository
    private var quickTask: Task<*>? = null
    private var remoteTask: Task<*>? = null
    private var max = 0

    // TODO: really synchronous?
    fun logMore() {
        val result = gitLog(repository, max, max + 50).filter { commits.none(it::equals) }
        if (result.isNotEmpty()) {
            max += 50
            commits.addAll(result)
            FXCollections.sort(commits)
        }
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        onRepositoryDeselected()
        update(repository)
    }

    override fun onRepositoryDeselected() {
        max = 50
        quickTask?.cancel()
        remoteTask?.cancel()
        commits.clear()
        activeCommit.set(null)
    }

    private fun update(repository: Repository) {
        this.repository = repository
        log()
    }

    private fun log() {
        quickTask?.cancel()
        remoteTask?.cancel()
        quickTask = object : Task<List<Commit>>() {
            override fun call() = gitLog(repository, 0, max)

            override fun succeeded() {
                commits.addAll(value.filter { commits.none(it::equals) })
                commits.removeAll(commits.filter { value.none(it::equals) })
                FXCollections.sort(commits)
                logRemote()
            }
        }.also { TinyGit.execute(it) }
    }

    private fun logRemote() {
        if (!service.hasRemote.get() || gitUpToDate(repository)) return
        remoteTask = object : Task<List<Commit>>() {
            override fun call(): List<Commit> {
                gitFetch(repository)
                return gitLog(repository, 0, max)
            }

            override fun succeeded() {
                commits.addAll(value.filter { commits.none(it::equals) })
                commits.removeAll(commits.filter { value.none(it::equals) })
                FXCollections.sort(commits)
                TinyGit.fireEvent()
            }

            override fun failed() {
                when (exception) {
                    is TimeoutException -> timeoutHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        }.also { logExecutor.execute(it) }
    }

}
