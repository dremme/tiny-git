package hamburg.remme.tinygit

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

    fun execute(task: Task<*>) {
        cachedThreadPool.execute(task)
    }

    fun stop() {
        cachedThreadPool.shutdownNow()
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * RUNNING PROCESSES                                                                                             *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val runningProcesses = SimpleIntegerProperty(0)
    private val processTextProperty = ReadOnlyStringWrapper()

    fun addProcess(message: String) {
        processTextProperty.set(message)
        runningProcesses.value += 1
    }

    fun removeProcess() {
        runningProcesses.value -= 1
    }

    fun processTextProperty() = processTextProperty.readOnlyProperty!!

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
    val stagedFiles = SimpleIntegerProperty()
    val pendingFiles = SimpleIntegerProperty()
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
    val canSettings = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canCommit = selectedRepositoryProperty.isNotNull.and(stagedFiles.greater0()).and(runningProcesses.equals0())!!
    val canPush = selectedRepositoryProperty.isNotNull.and(aheadProperty.notEquals0()).and(runningProcesses.equals0())!!
    val canPull = selectedRepositoryProperty.isNotNull.and(behindProperty.greater0()).and(runningProcesses.equals0())!!
    val canFetch = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canTag = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!
    val canBranch = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())!!
    val canMerge = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!
    val canStash = selectedRepositoryProperty.isNotNull.and(runningProcesses.equals0())
            .and(stagedFiles.greater0()).or(pendingFiles.greater0())!!
    val canApplyStash = selectedRepositoryProperty.isNotNull.and(stashEntries.greater0()).and(runningProcesses.equals0())!!
    val canReset = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!
    val canSquash = FALSE // TODO: selectedRepositoryProperty.isNotNull.and(runningProcesses.isZero())!!

    val canStageAll = pendingFiles.greater0()!!
    val canStageSelected = pendingFilesSelected.greater0()!!
    val canUnstageAll = stagedFiles.greater0()!!
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
