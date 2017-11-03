package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.FXCollections
import java.util.concurrent.Executors

object State {

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * THREAD POOLS                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val cachedThreadPool = Executors.newCachedThreadPool()!!

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * RUNNING PROCESSES                                                                                             *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val runningProcesses = ReadOnlyIntegerWrapper(0)

    fun runningProcessesProperty() = runningProcesses.readOnlyProperty!!

    fun addProcess() {
        runningProcesses.value += 1
    }

    fun removeProcess() {
        runningProcesses.value -= 1
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REPOSITORIES                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
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

    fun getSelectedRepository(): LocalRepository? = selectedRepository.readOnlyProperty.value

    fun setSelectedRepository(repository: LocalRepository) {
        selectedRepository.set(repository)
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * VISIBILITY                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val showInfo = Bindings.isEmpty(repositories)!!

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val canCommit = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canPush = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canPull = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canFetch = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canTag = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canBranch = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canMerge = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canStash = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canApplyStash = selectedRepository.isNotNull.and(runningProcesses.isZero())!!
    val canReset = selectedRepository.isNotNull.and(runningProcesses.isZero())!!

    private fun IntegerProperty.isZero() = this.isEqualTo(0)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * LISTENERS                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val refreshListeners = mutableListOf<() -> Unit>()
    private val focusListeners = mutableListOf<() -> Unit>()


    fun addRefreshListener(listener: () -> Unit) {
        refreshListeners += listener
    }

    fun removeRefreshListener(listener: () -> Unit) {
        refreshListeners -= listener
    }

    fun fireRefresh() {
        refreshListeners.forEach { it.invoke() }
    }

    fun addFocusListener(listener: () -> Unit) {
        focusListeners += listener
    }

    fun removeFocusListener(listener: () -> Unit) {
        focusListeners -= listener
    }

    fun fireFocus() {
        focusListeners.forEach { it.invoke() }
    }

}
