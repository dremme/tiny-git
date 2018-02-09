package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.addSorted
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.FetchException
import hamburg.remme.tinygit.git.gitFetch
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.git.gitUpToDate
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task

class CommitLogService(private val service: RepositoryService) : Refreshable {

    val commits = observableList<Commit>()
    val activeCommit = SimpleObjectProperty<Commit?>()
    val scope = object : SimpleObjectProperty<Scope>(Scope.ALL) {
        override fun invalidated() = log()
    }
    val commitType = object : SimpleObjectProperty<CommitType>(CommitType.ALL) {
        override fun invalidated() = log()
    }
    lateinit var logExecutor: TaskExecutor
    lateinit var logErrorHandler: (String) -> Unit
    private lateinit var repository: Repository
    private var quickTask: Task<*>? = null
    private var remoteTask: Task<*>? = null
    private val maxIncrement = 30
    private var max = 0

    fun logMore() {
        val result = gitLog(repository, scope.get().isAll, commitType.get().isNoMerges, max, max + maxIncrement)
                .filter { commits.none(it::equals) }
        if (result.isNotEmpty()) {
            max += maxIncrement
            commits.addSorted(result)
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
        max = maxIncrement
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
            override fun call() = gitLog(repository, scope.get().isAll, commitType.get().isNoMerges, 0, max)

            override fun succeeded() {
                commits.addSorted(value.filter { commits.none(it::equals) })
                commits.removeAll(commits.filter { value.none(it::equals) })
                logRemote()
            }
        }.also { TinyGit.execute(it) }
    }

    private fun logRemote() {
        if (!service.hasRemote.get() || gitUpToDate(repository)) return
        remoteTask = object : Task<List<Commit>>() {
            override fun call(): List<Commit> {
                gitFetch(repository)
                return gitLog(repository, scope.get().isAll, commitType.get().isNoMerges, 0, max)
            }

            override fun succeeded() {
                commits.addSorted(value.filter { commits.none(it::equals) })
                commits.removeAll(commits.filter { value.none(it::equals) })
                TinyGit.fireEvent()
            }

            override fun failed() {
                when (exception) {
                    is FetchException -> logErrorHandler.invoke(exception.message!!)
                    else -> exception.printStackTrace()
                }
            }
        }.also { logExecutor.execute(it) }
    }

    enum class Scope(val isAll: Boolean, private val description: String) {

        ALL(true, "All Branches"), CURRENT(false, "Current Branch Only");

        override fun toString() = description

    }

    enum class CommitType(val isNoMerges: Boolean, private val description: String) {

        ALL(false, "All Commits"), NO_MERGE_COMMITS(true, "No Merges");

        override fun toString() = description

    }

}
