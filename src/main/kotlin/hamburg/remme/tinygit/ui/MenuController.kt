package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
import hamburg.remme.tinygit.openURI
import hamburg.remme.tinygit.event.QuitEvent
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import hamburg.remme.tinygit.isGitRepository
import hamburg.remme.tinygit.logger
import javafx.scene.control.MenuBar
import javafx.stage.DirectoryChooser
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Controller

/**
 * Controller handling the [MenuBar].
 */
@Controller class MenuController(private val publisher: ApplicationEventPublisher, private val context: Context) {

    private val log = logger<MenuController>()

    /**
     * On action `Open...`.
     */
    fun onOpen() {
        DirectoryChooser().showDialog(context.window)?.let {
            if (it.isGitRepository()) {
                log.info("Opening repository $it.")
                publisher.publishEvent(RepositoryOpenedEvent(it))
            } else {
                log.info("$it is not a Git repository.")
            }
        }
    }

    /**
     * On action `Close...`.
     */
    fun onClose() {
        log.info("Closing repository.")
        publisher.publishEvent(RepositoryClosedEvent())
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
