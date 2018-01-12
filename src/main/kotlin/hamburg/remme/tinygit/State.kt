package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.domain.Repository
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task
import java.util.concurrent.Executors

object State {

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * THREAD POOLS                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val cachedThreadPool = Executors.newCachedThreadPool({
        Executors.defaultThreadFactory().newThread(it).apply { isDaemon = true }
    })
    private val runningProcesses = SimpleIntegerProperty(0)
    private val processText = ReadOnlyStringWrapper()

    fun processTextProperty() = processText.readOnlyProperty!!

    fun execute(task: Task<*>) = cachedThreadPool.execute(task)

    fun startProcess(message: String, task: Task<*>) {
        task.setOnSucceeded { runningProcesses.dec() }
        task.setOnCancelled { runningProcesses.dec() }
        task.setOnFailed { runningProcesses.dec() }
        processText.set(message)
        runningProcesses.inc()
        cachedThreadPool.execute(task)
    }

    private fun IntegerProperty.inc() = set(get() + 1)
    private fun IntegerProperty.dec() = set(get() - 1)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REPOSITORIES                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val allRepositories = observableList<Repository>()
    private val repositories = allRepositories.filtered { it.path.asPath().exists() }!!
    val selectedRepository = SimpleObjectProperty<Repository>()
    val branchCount = SimpleIntegerProperty()
    val aheadDefault = SimpleIntegerProperty()
    val ahead = SimpleIntegerProperty()
    val behind = SimpleIntegerProperty()

    fun getAllRepositories() = allRepositories

    fun getRepositories() = repositories

    fun setRepositories(repositories: Collection<Repository>) = allRepositories.setAll(repositories)

    fun addRepository(repository: Repository) {
        if (!allRepositories.contains(repository)) allRepositories.add(repository)
    }

    fun removeRepository(repository: Repository) = allRepositories.remove(repository)

    fun getSelectedRepository() = selectedRepository.get()!!

    fun setSelectedRepository(repository: Repository?) = selectedRepository.set(repository)

    fun addRepositoryListener(block: (Repository?) -> Unit) = selectedRepository.addListener { _, _, it -> block.invoke(it) }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * WORKING COPY                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val isMerging = SimpleBooleanProperty()
    val isRebasing = SimpleBooleanProperty()
    val rebaseNext = SimpleIntegerProperty()
    val rebaseLast = SimpleIntegerProperty()
    val stagedFiles = observableList<GitFile>()
    val pendingFiles = observableList<GitFile>()
    val stagedSelectedCount = SimpleIntegerProperty()
    val pendingSelectedCount = SimpleIntegerProperty()
    val stashSize = SimpleIntegerProperty()
    val commitMessage = SimpleStringProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * VISIBILITY                                                                                                    *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val showGlobalInfo = Bindings.isEmpty(repositories)!!
    val showGlobalOverlay = runningProcesses.greater0()!!
    val showToolBar = isMerging.not().and(isRebasing.not())!!
    val showMergeBar = isMerging
    val showRebaseBar = isRebasing
    val isModal = SimpleBooleanProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val isIdle = selectedRepository.isNotNull.and(runningProcesses.equals0())!!
    private val isReady = isIdle.and(isMerging.not()).and(isRebasing.not())!!
    val canRemove = isReady
    val canSettings = isReady
    val canCommit = isReady.and(Bindings.isNotEmpty(stagedFiles))!!
    val canPush = isReady.and(ahead.unequals0())!!
    val canForcePush = canPush
    val canPull = isReady.and(behind.greater0())!!
    val canFetch = isReady
    val canGc = canFetch
    val canBranch = isReady
    val canMerge = isReady.and(branchCount.greater1())!!
    val canMergeContinue = isIdle.and(isMerging)!!
    val canMergeAbort = isIdle.and(isMerging)!!
    val canRebase = isReady.and(branchCount.greater1())!!
    val canRebaseContinue = isIdle.and(isRebasing)!!
    val canRebaseAbort = isIdle.and(isRebasing)!!
    val canStash = isReady.and(Bindings.isNotEmpty(stagedFiles).or(Bindings.isNotEmpty(pendingFiles)))!!
    val canApplyStash = isReady.and(stashSize.greater0())!!
    val canReset = isReady.and(behind.greater0())!!
    val canSquash = isReady.and(aheadDefault.greater1())!!

    val canStageAll = isIdle.and(Bindings.isNotEmpty(pendingFiles))!!
    val canUpdateAll = isIdle.and(Bindings.isNotEmpty(pendingFiles.filtered { it.status != GitFile.Status.ADDED }))!!
    val canStageSelected = isIdle.and(pendingSelectedCount.greater0())!!
    val canUnstageAll = isIdle.and(Bindings.isNotEmpty(stagedFiles))!!
    val canUnstageSelected = isIdle.and(stagedSelectedCount.greater0())!!

    private fun IntegerProperty.equals0() = isEqualTo(0)
    private fun IntegerProperty.unequals0() = isNotEqualTo(0)
    private fun IntegerProperty.greater0() = greaterThan(0)
    private fun IntegerProperty.greater1() = greaterThan(1)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REFRESH                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val refreshListeners = mutableMapOf<Any, (Repository) -> Unit>()

    fun addRefreshListener(receiver: Any, block: (Repository) -> Unit) {
        refreshListeners[receiver] = block
    }

    fun fireRefresh(source: Any) {
        selectedRepository.get()?.let { repository ->
            refreshListeners.forEach { receiver, it ->
                if (source !== receiver) it.invoke(repository)
            }
        }
    }

}
