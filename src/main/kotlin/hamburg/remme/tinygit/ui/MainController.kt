package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.domain.RepositoryService
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import hamburg.remme.tinygit.system.git.Commit
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.fxml.FXML
import javafx.scene.control.ListView
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller

/**
 * Controller handling the general application.
 */
@Controller class MainController(private val service: RepositoryService) {

    @FXML private lateinit var commitListView: ListView<Commit>

    private val noRepositoryProperty = ReadOnlyBooleanWrapper(true)
    fun noRepositoryProperty(): ReadOnlyBooleanProperty = noRepositoryProperty.readOnlyProperty
    /**
     * `true` when there is no opened Git repository.
     */
    val noRepository: Boolean get() = noRepositoryProperty.value

    /**
     * Handles a repository being opened.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryOpened(event: RepositoryOpenedEvent) {
        commitListView.items.setAll(service.list(event.directory))
        noRepositoryProperty.value = false
    }

    /**
     * Handles a repository being closed.
     * @param event the event.
     */
    @EventListener fun handleRepositoryClosed(event: RepositoryClosedEvent) {
        noRepositoryProperty.value = true
        commitListView.items.clear()
    }

}
