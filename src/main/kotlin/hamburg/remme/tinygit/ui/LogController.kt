package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.domain.RepositoryService
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import hamburg.remme.tinygit.event.RepositoryUpdatedEvent
import hamburg.remme.tinygit.logger
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.ui.list.LogCell
import hamburg.remme.tinygit.ui.list.LogCellCallback
import javafx.fxml.FXML
import javafx.scene.control.ListView
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller
import java.io.File

/**
 * Controller handling the log view.
 */
@Controller class LogController(private val service: RepositoryService,
                                private val settings: Settings,
                                private val context: Context) {

    /**
     * Cell factory for creating log cells. Used by the FXML loader.
     */
    val logCellFactory: LogCellCallback by lazy { LogCellCallback { LogCell(settings, context.resources) } }

    private val log = logger<LogController>()
    @FXML private lateinit var commitListView: ListView<Commit>

    /**
     * Handles a repository being opened.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryOpened(event: RepositoryOpenedEvent) {
        updateLog(event.directory)
    }

    /**
     * Handles a repository being updated.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryUpdated(event: RepositoryUpdatedEvent) {
        updateLog(event.directory)
    }

    /**
     * Handles a repository being closed.
     * @param event the event.
     */
    @EventListener fun handleRepositoryClosed(event: RepositoryClosedEvent) {
        commitListView.items.clear()
    }

    private fun updateLog(directory: File) {
        val commits = service.list(directory)
        log.info("Showing ${commits.count()} commits in the log.")
        commitListView.items.setAll(commits.toList())
    }

}
