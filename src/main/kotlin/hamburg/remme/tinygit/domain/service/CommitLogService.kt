package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
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

object CommitLogService : Refreshable {

    val commits = observableList<Commit>()
    val activeCommit = SimpleObjectProperty<Commit?>()
    lateinit var timeoutHandler: () -> Unit
    private lateinit var repository: Repository
    private var quickTask: Task<*>? = null
    private var remoteTask: Task<*>? = null

    fun log() {
        quickTask?.cancel()
        remoteTask?.cancel()
        quickTask = object : Task<List<Commit>>() {
            override fun call() = gitLog(repository, 0, 50)

            override fun succeeded() {
                commits.addAll(value.filter { commits.none(it::equals) })
                commits.removeAll(commits.filter { value.none(it::equals) })
                FXCollections.sort(commits)
                logRemote()
            }
        }.also { State.execute(it) }
    }

    // TODO: re-enable log more
//     fun logMore(repository: Repository) {
//        if (localCommits.items.size < skipSize) return
//
//        val commits = gitLog(repository, skip + skipSize, logSize)
//        if (commits.isNotEmpty()) {
//            skip += skipSize
//            localCommits.items.addAll(commits)
//            localCommits.scrollTo(skip - 1)
//        }
//    }

    fun logRemote() {
        if (!RepositoryService.hasRemote.get() || gitUpToDate(repository)) return
        remoteTask = object : Task<List<Commit>>() {
            override fun call(): List<Commit> {
                gitFetch(repository)
                return gitLog(repository, 0, 50)
            }

            override fun succeeded() {
                commits.addAll(value.filter { commits.none(it::equals) })
                commits.removeAll(commits.filter { value.none(it::equals) })
                FXCollections.sort(commits)
                State.fireRefresh()
            }

            override fun failed() {
                when (exception) {
                    is TimeoutException -> timeoutHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        }.also { State.execute(it) }
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        commits.clear()
        activeCommit.set(null)
    }

    private fun update(repository: Repository) {
        this.repository = repository
        log()
    }

}
