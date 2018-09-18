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
import javafx.scene.control.MenuBar
import javafx.stage.DirectoryChooser
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Controller

/**
 * Controller handling the [MenuBar].
 */
@Controller class MenuController(private val publisher: ApplicationEventPublisher,
                                 private val settings: Settings,
                                 private val context: Context) {

    private val emptyProperty = ReadOnlyBooleanWrapper(true)
    fun emptyProperty(): ReadOnlyBooleanProperty = emptyProperty.readOnlyProperty // FIXME: not working on menu item??
    /**
     * `true` when there is no opened Git repository.
     */
    val isEmpty: Boolean get() = emptyProperty.value

    private val log = logger<MenuController>()

    /**
     * On action `Open...`.
     */
    fun onOpen() {
        DirectoryChooser().showDialog(context.window)?.let {
            if (it.isGitRepository()) {
                log.info("Opening $it.")
                settings.repository = it
                emptyProperty.value = false
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
        emptyProperty.value = true
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

}
