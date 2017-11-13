package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleIntegerProperty
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

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * RUNNING PROCESSES                                                                                             *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val runningProcesses = ReadOnlyIntegerWrapper(0)
    private val processText = ReadOnlyStringWrapper()

    fun addProcess(message: String) {
        processText.set(message)
        runningProcesses.value += 1
    }

    fun removeProcess() {
        runningProcesses.value -= 1
    }

    fun processTextProperty() = processText.readOnlyProperty!!

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REPOSITORIES                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val ahead = ReadOnlyIntegerWrapper()
    val behind = ReadOnlyIntegerWrapper()
    private val repositories = FXCollections.observableArrayList<LocalRepository>()!!
    private val selectedRepository = ReadOnlyObjectWrapper<LocalRepository>()

    fun getRepositories() = FXCollections.unmodifiableObservableList(repositories)!!

    fun setRepositories(repositories: Collection<LocalRepository>) {
        this.repositories.setAll(repositories)
    }

    fun addRepository(repository: LocalRepository) {
        this.repositories += repository
    }

    fun selectedRepositoryProperty() = selectedRepository.readOnlyProperty!!

    fun getSelectedRepository(): LocalRepository = selectedRepository.readOnlyProperty.value!!

    fun getSelectedRepository(block: (LocalRepository) -> Unit) = selectedRepository.readOnlyProperty.value?.let(block)

    fun setSelectedRepository(repository: LocalRepository) {
        selectedRepository.set(repository)
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * WORKING COPY                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val stagedFiles = SimpleIntegerProperty()
    val unstagedFiles = SimpleIntegerProperty()
    val stashEntries = SimpleIntegerProperty()
    val commitMessage = SimpleStringProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * VISIBILITY                                                                                                    *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val showGlobalInfo = Bindings.isEmpty(repositories)!!
    val showGlobalOverlay = runningProcesses.greaterThan(0)!!

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val canSettings = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canCommit = selectedRepository.isNotNull.and(stagedFiles.isGreaterZero()).and(runningProcesses.isZero())!!
    val canPush = selectedRepository.isNotNull.and(ahead.isGreaterZero()).and(runningProcesses.isZero())!!
    val canPull = selectedRepository.isNotNull.and(behind.isGreaterZero()).and(runningProcesses.isZero())!!
    val canFetch = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canTag = FALSE // TODO: selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canBranch = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canMerge = FALSE // TODO: selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canStash = selectedRepository.isNotNull.and(runningProcesses.isZero())
            .and(stagedFiles.isGreaterZero()).or(unstagedFiles.isGreaterZero())!!
    val canApplyStash = selectedRepository.isNotNull.and(stashEntries.isGreaterZero()).and(runningProcesses.isZero())!!
    val canReset = FALSE // TODO: selectedRepository.isNotNull.and(runningProcesses.isZero())!!

    private fun IntegerProperty.isZero() = this.isEqualTo(0)
    private fun IntegerProperty.isGreaterZero() = this.greaterThan(0)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * LISTENERS                                                                                                     *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val refreshListeners = mutableListOf<() -> Unit>()
    private val focusListeners = mutableListOf<() -> Unit>()


    fun addRefreshListener(block: () -> Unit) {
        refreshListeners += block
    }

    fun removeRefreshListener(block: () -> Unit) {
        refreshListeners -= block
    }

    fun fireRefresh() {
        refreshListeners.forEach { it.invoke() }
    }

    fun addFocusListener(block: () -> Unit) {
        focusListeners += block
    }

    fun removeFocusListener(block: () -> Unit) {
        focusListeners -= block
    }

    fun fireFocus() {
        focusListeners.forEach { it.invoke() }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * LISTENERS                                                                                                     *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    fun stop() {
        cachedThreadPool.shutdownNow()
    }

}
