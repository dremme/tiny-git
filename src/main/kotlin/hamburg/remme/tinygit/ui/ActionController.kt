package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.event.QuitEvent
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import hamburg.remme.tinygit.event.RepositoryUpdateRequestedEvent
import hamburg.remme.tinygit.isGitRepository
import hamburg.remme.tinygit.logger
import hamburg.remme.tinygit.openURI
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.stage.DirectoryChooser
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener

abstract class ActionController(private val publisher: ApplicationEventPublisher,
                                private val settings: Settings,
                                private val context: Context) {

    private val emptyProperty: ReadOnlyBooleanWrapper = ReadOnlyBooleanWrapper(true)
    fun emptyProperty(): ReadOnlyBooleanProperty = emptyProperty.readOnlyProperty
    /**
     * `true` when there is no opened Git repository.
     */
    val isEmpty: Boolean get() = emptyProperty.value

    private val log = logger<ActionController>()

    /**
     * On action `Open...`.
     */
    fun onOpen() {
        DirectoryChooser().showDialog(context.window)?.let {
            if (it.isGitRepository()) {
                log.info("Opening $it.")
                settings.repository = it
                publisher.publishEvent(RepositoryOpenedEvent(it))
            } else {
                log.info("$it is not a Git repository.")
            }
        }
    }

    /**
     * On action `Close.`.
     */
    fun onClose() {
        log.info("Closing ${settings.repository}.")
        publisher.publishEvent(RepositoryClosedEvent())
        settings.repository = null // after sending the event
    }

    /**
     * On action 'Pull'.
     */
    fun onUpdate() {
        log.info("Updating ${settings.repository}.")
        publisher.publishEvent(RepositoryUpdateRequestedEvent(settings.repository!!))
    }

    /**
     * On action 'Quit'.
     */
    fun onQuit() {
        log.info("Quitting application.")
        publisher.publishEvent(QuitEvent())
    }

    /**
     * On action 'Star on GitHub'.
     */
    fun onGithub() {
        openURI("https://github.com/dremme/tiny-git")
    }

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

}
