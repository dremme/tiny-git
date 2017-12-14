package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.concurrent.WorkerStateEvent
import javafx.event.EventHandler
import java.util.concurrent.Executors

object State {

    private val TRUE = ReadOnlyBooleanWrapper(true).readOnlyProperty!!
    private val FALSE = ReadOnlyBooleanWrapper(false).readOnlyProperty!!

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * THREAD POOLS                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val cachedThreadPool = Executors.newCachedThreadPool()
    private val processStopped = EventHandler<WorkerStateEvent> { runningProcesses.dec() }
    private val runningProcesses = SimpleIntegerProperty(0)
    private val processText = ReadOnlyStringWrapper()

    fun processTextProperty() = processText.readOnlyProperty!!

    fun execute(task: Task<*>) = cachedThreadPool.execute(task)

    fun startProcess(message: String, task: Task<*>) {
        processText.set(message)
        runningProcesses.inc()
        task.onCancelled = processStopped
        task.onFailed = processStopped
        task.onSucceeded = processStopped
        cachedThreadPool.execute(task)
    }

    fun stop() {
        cachedThreadPool.shutdownNow()
    }

    private fun IntegerProperty.inc() = set(get() + 1)
    private fun IntegerProperty.dec() = set(get() - 1)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REPOSITORIES                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val allRepositories = FXCollections.observableArrayList<LocalRepository>()!!
    private val repositories = allRepositories.filtered { it.path.asPath().exists() }!!
    val selectedRepository = SimpleObjectProperty<LocalRepository>()
    val ahead = SimpleIntegerProperty()
    val behind = SimpleIntegerProperty()
    val featureBranch = SimpleBooleanProperty()
    val merging = SimpleBooleanProperty()
    val rebasing = SimpleBooleanProperty()
    val rebaseNext = SimpleIntegerProperty()
    val rebaseLast = SimpleIntegerProperty()

    fun getAllRepositories() = allRepositories

    fun getRepositories() = repositories

    fun setRepositories(repositories: Collection<LocalRepository>) = allRepositories.setAll(repositories)

    fun addRepository(repository: LocalRepository) {
        if (!allRepositories.contains(repository)) allRepositories.add(repository)
    }

    fun removeRepository(repository: LocalRepository) = allRepositories.remove(repository)

    fun getSelectedRepository() = selectedRepository.get()!!

    fun setSelectedRepository(repository: LocalRepository?) = selectedRepository.set(repository)

    fun addRepositoryListener(block: (LocalRepository?) -> Unit) = selectedRepository.addListener { _, _, it -> block.invoke(it) }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * WORKING COPY                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val stagedFiles = FXCollections.observableArrayList<LocalFile>()!!
    val pendingFiles = FXCollections.observableArrayList<LocalFile>()!!
    val stagedFilesSelected = SimpleIntegerProperty()
    val pendingFilesSelected = SimpleIntegerProperty()
    val stashEntries = SimpleIntegerProperty()
    val commitMessage = SimpleStringProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * VISIBILITY                                                                                                    *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val showGlobalInfo = Bindings.isEmpty(repositories)!!
    val showGlobalOverlay = runningProcesses.greater0()!!
    val showToolBar = merging.not().and(rebasing.not())!!
    val showMergeBar = merging
    val showRebaseBar = rebasing
    val modalVisible = SimpleBooleanProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val isIdle = selectedRepository.isNotNull.and(runningProcesses.equals0())!!
    private val isReady = isIdle.and(merging.not()).and(rebasing.not())!!
    val canRemove = isReady
    val canSettings = isReady
    val canCommit = isReady.and(Bindings.isNotEmpty(stagedFiles))!!
    val canPush = isReady.and(ahead.unequals0())!!
    val canPull = isReady.and(behind.greater0())!!
    val canFetch = isReady
    val canTag = FALSE // TODO
    val canBranch = isReady
    val canMerge = FALSE // TODO
    val canRebase = isReady
    val canRebaseContinue = isIdle.and(rebasing)!!
    val canRebaseAbort = isIdle.and(rebasing)!!
    val canStash = isReady.and(Bindings.isNotEmpty(stagedFiles).or(Bindings.isNotEmpty(pendingFiles)))!!
    val canApplyStash = isReady.and(stashEntries.greater0())!!
    val canReset = isReady.and(behind.greater0())!!
    val canSquash = isReady.and(featureBranch).and(ahead.greater1())!!

    val canStageAll = Bindings.isNotEmpty(pendingFiles)!!
    val canUpdateAll = Bindings.isNotEmpty(pendingFiles.filtered { it.status != LocalFile.Status.ADDED && !it.cached })!!
    val canStageSelected = pendingFilesSelected.greater0()!!
    val canUnstageAll = Bindings.isNotEmpty(stagedFiles)!!
    val canUnstageSelected = stagedFilesSelected.greater0()!!

    private fun IntegerProperty.equals0() = isEqualTo(0)
    private fun IntegerProperty.unequals0() = isNotEqualTo(0)
    private fun IntegerProperty.greater0() = greaterThan(0)
    private fun IntegerProperty.greater1() = greaterThan(1)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REFRESH                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val refreshListeners = mutableListOf<(LocalRepository) -> Unit>()

    fun addRefreshListener(block: (LocalRepository) -> Unit) {
        refreshListeners += block
    }

    fun fireRefresh() {
        selectedRepository.get()?.let { repo -> refreshListeners.forEach { it.invoke(repo) } }
    }

}
