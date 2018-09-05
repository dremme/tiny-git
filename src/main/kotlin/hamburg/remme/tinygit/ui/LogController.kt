package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
import hamburg.remme.tinygit.domain.RepositoryService
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import hamburg.remme.tinygit.logger
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.ui.list.LogCell
import hamburg.remme.tinygit.ui.list.LogCellCallback
import javafx.fxml.FXML
import javafx.scene.control.ListView
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller

/**
 * Controller handling the log view.
 */
@Controller class LogController(private val service: RepositoryService, private val context: Context) {

    /**
     * Cell factory for creating log cells. Used by the FXML loader.
     */
    @Suppress("unused") val logCellFactory: LogCellCallback by lazy {
        LogCellCallback { LogCell(context.resources) }
    }
    private val log = logger<LogController>()
    @FXML private lateinit var commitListView: ListView<Commit>

    /**
     * Handles a repository being opened.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryOpened(event: RepositoryOpenedEvent) {
        val commits = service.list(event.directory)
        log.info("Showing {} commits in the log.", commits.size)
        commitListView.items.setAll(commits)
    }

    /**
     * Handles a repository being closed.
     * @param event the event.
     */
    @EventListener fun handleRepositoryClosed(event: RepositoryClosedEvent) {
        commitListView.items.clear()
    }

}
