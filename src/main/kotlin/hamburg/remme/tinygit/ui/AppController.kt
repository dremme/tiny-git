package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.event.QuitEvent
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import javafx.application.Platform
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller

/**
 * Controller handling the general application.
 */
@Controller class AppController {

    private val emptyProperty = ReadOnlyBooleanWrapper(true)
    fun emptyProperty(): ReadOnlyBooleanProperty = emptyProperty.readOnlyProperty
    /**
     * `true` when there is no opened Git repository.
     */
    val isEmpty: Boolean get() = emptyProperty.value

    /**
     * Handles a repository being opened.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryOpened(event: RepositoryOpenedEvent) {
        emptyProperty.value = false
    }

    /**
     * Handles a repository being closed.
     * @param event the event.
     */
    @EventListener fun handleRepositoryClosed(event: RepositoryClosedEvent) {
        emptyProperty.value = true
    }

    /**
     * Handles the application shutdown.
     * @param event the event.
     */
    @EventListener fun handleQuit(event: QuitEvent) {
        Platform.exit()
    }

}
