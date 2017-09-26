package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.FXCollections
import java.util.concurrent.Executors

object State {

    val cachedThreadPool = Executors.newCachedThreadPool()!!
    private val repositories = FXCollections.observableArrayList<LocalRepository>()!!
    private val selectedRepository = ReadOnlyObjectWrapper<LocalRepository>()
    private val refreshListeners = mutableListOf<() -> Unit>()

    fun getRepositories() = FXCollections.unmodifiableObservableList(repositories)!!

    fun setRepositories(repositories: Collection<LocalRepository>) {
        this.repositories.setAll(repositories)
    }

    fun addRepository(repository: LocalRepository) {
        this.repositories += repository
    }

    fun selectedRepositoryProperty() = selectedRepository.readOnlyProperty!!

    fun getSelectedRepository() = selectedRepository.readOnlyProperty.value!!

    fun setSelectedRepository(repository: LocalRepository) {
        selectedRepository.set(repository)
    }

    fun hasSelectedRepository() = selectedRepository.readOnlyProperty.value != null

    fun addRefreshListener(listener: () -> Unit) {
        refreshListeners += listener
    }

    fun removeRefreshListener(listener: () -> Unit) {
        refreshListeners -= listener
    }

    fun fireRefresh() {
        refreshListeners.forEach { it.invoke() }
    }

}
