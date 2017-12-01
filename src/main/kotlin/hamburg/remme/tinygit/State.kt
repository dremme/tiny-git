package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyIntegerWrapper
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
    private val FALSE = ReadOnlyBooleanWrapper().readOnlyProperty!!

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * THREAD POOLS                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val cachedThreadPool = Executors.newCachedThreadPool()
    private val processStopped = EventHandler<WorkerStateEvent> { runningProcesses.value -= 1 }
    private val runningProcesses = SimpleIntegerProperty(0)
    private val processTextProperty = ReadOnlyStringWrapper()

    fun processTextProperty() = processTextProperty.readOnlyProperty!!

    fun execute(task: Task<*>) = cachedThreadPool.execute(task)

    fun startProcess(message: String, task: Task<*>) {
        processTextProperty.set(message)
        runningProcesses.value += 1
        task.onCancelled = processStopped
        task.onFailed = processStopped
        task.onSucceeded = processStopped
        cachedThreadPool.execute(task)
    }

    fun stop() {
        cachedThreadPool.shutdownNow()
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REPOSITORIES                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    var ahead: Int
        get() = aheadProperty.get()
        set(value) = aheadProperty.set(value)
    var behind: Int
        get() = behindProperty.get()
        set(value) = behindProperty.set(value)
    val repositories = FXCollections.observableArrayList<LocalRepository>()!!
    var selectedRepository: LocalRepository
        get() = selectedRepositoryProperty.get()
        set(value) = selectedRepositoryProperty.set(value)
    private val aheadProperty = ReadOnlyIntegerWrapper()
    private val behindProperty = ReadOnlyIntegerWrapper()
    private val selectedRepositoryProperty = SimpleObjectProperty<LocalRepository>()

    fun aheadProperty() = aheadProperty.readOnlyProperty!!

    fun behindProperty() = behindProperty.readOnlyProperty!!

    fun addRepositoryListener(block: (LocalRepository?) -> Unit) {
        selectedRepositoryProperty.addListener { _, _, it -> block.invoke(it) }
    }

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
    val showGlobalOverlay = runningProcesses.greaterThan(0)!!
    val modalVisible = SimpleBooleanProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val canRemove = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canSettings = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canCommit = selectedRepositoryProperty.isNotNull.and(Bindings.isNotEmpty(stagedFiles)).and(runningProcesses.equals0())!!
    val canPush = selectedRepositoryProperty.isNotNull.and(aheadProperty.notEquals0()).and(runningProcesses.equals0())!!
    val canPull = selectedRepositoryProperty.isNotNull.and(behindProperty.greater0()).and(runningProcesses.equals0())!!
    val canFetch = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canTag = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!
    val canBranch = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canMerge = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!
    val canStash = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())
            .and(Bindings.isNotEmpty(stagedFiles)).or(Bindings.isNotEmpty(pendingFiles))!!
    val canApplyStash = selectedRepositoryProperty.isNotNull.and(stashEntries.greater0()).and(runningProcesses.equals0())!!
    val canReset = selectedRepositoryProperty.isNotNull.and(behindProperty.greater0()).and(runningProcesses.equals0())!!
    val canSquash = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!

    val canStageAll = Bindings.isNotEmpty(pendingFiles)!!
    val canUpdateAll = Bindings.isNotEmpty(pendingFiles.filtered { it.status != LocalFile.Status.ADDED && !it.cached })!!
    val canStageSelected = pendingFilesSelected.greater0()!!
    val canUnstageAll = Bindings.isNotEmpty(stagedFiles)!!
    val canUnstageSelected = stagedFilesSelected.greater0()!!

    private fun IntegerProperty.equals0() = isEqualTo(0)
    private fun IntegerProperty.notEquals0() = isNotEqualTo(0)
    private fun IntegerProperty.greater0() = greaterThan(0)

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
        selectedRepositoryProperty.get()?.let { repo ->
            refreshListeners.forEach { it.invoke(repo) }
        }
    }

}
