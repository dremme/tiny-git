package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.addSorted
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.LogGraph
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.execute
import hamburg.remme.tinygit.git.FetchException
import hamburg.remme.tinygit.git.gitFetch
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.git.gitUpToDate
import hamburg.remme.tinygit.observableList
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task

class CommitLogService(private val repositoryService: RepositoryService,
                       private val credentialService: CredentialService) : Refreshable {

    val commits = observableList<Commit>()
    val activeCommit = SimpleObjectProperty<Commit?>()
    val scope = object : SimpleObjectProperty<Scope>(Scope.ALL) {
        override fun invalidated() = log()
    }
    val commitType = object : SimpleObjectProperty<CommitType>(CommitType.ALL) {
        override fun invalidated() = log()
    }
    val logGraph = LogGraph() // TODO: overthink this
    lateinit var logListener: TaskListener
    lateinit var logErrorHandler: (String) -> Unit
    private lateinit var repository: Repository
    private var quickTask: Task<*>? = null
    private var remoteTask: Task<*>? = null
    private val maxIncrement = 250 // TODO: magic number
    private var max = 0

    // TODO: too buggy and needy right now.
//    fun logMore() {
//        val result = gitLog(repository, scope.get().isAll, commitType.get().isNoMerges, max, max + maxIncrement)
//                .filter { commits.none(it::equals) }
//        if (result.isNotEmpty()) {
//            max += maxIncrement
//            commits.addSorted(result)
//        }
//    }

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
            override fun call(): List<Commit> {
                val log = gitLog(repository, scope.get().isAll, commitType.get().isNoMerges, 0, max)
                logGraph.recreate(log)
                return log
            }

            override fun succeeded() {
                commits.addSorted(value.filter { commits.none(it::equals) })
                commits -= commits.filter { value.none(it::equals) }
                logRemote()
            }
        }.execute()
    }

    private fun logRemote() {
        if (!repositoryService.hasRemote.get() || gitUpToDate(repository)) return
        credentialService.applyCredentials(repositoryService.remote.get())
        remoteTask = object : Task<List<Commit>>() {
            override fun call(): List<Commit> {
                Platform.runLater { logListener.started() }
                gitFetch(repository)
                val log = gitLog(repository, scope.get().isAll, commitType.get().isNoMerges, 0, max)
                logGraph.recreate(log)
                return log
            }

            override fun succeeded() {
                commits.addSorted(value.filter { commits.none(it::equals) })
                commits -= commits.filter { value.none(it::equals) }
                TinyGit.fireEvent()
            }

            override fun failed() {
                when (exception) {
                    is FetchException -> logErrorHandler(exception.message!!)
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { logListener.done() }
        }.execute()
    }

    enum class Scope(val isAll: Boolean, private val description: String) {

        ALL(true, I18N["commitLog.allBranches"]), CURRENT(false, I18N["commitLog.currentOnly"]);

        override fun toString() = description

    }

    enum class CommitType(val isNoMerges: Boolean, private val description: String) {

        ALL(false, I18N["commitLog.allCommits"]), NO_MERGE_COMMITS(true, I18N["commitLog.noMerges"]);

        override fun toString() = description

    }

}
