package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
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
        val directory = DirectoryChooser().showDialog(context.window)
        if (directory.isGitRepository()) publisher.publishEvent(RepositoryOpenedEvent(directory))
        else log.info("{} is not a Git repository.", directory)
    }

    /**
     * On action `Close...`.
     */
    fun onClose() {
        publisher.publishEvent(RepositoryClosedEvent())
    }

}
