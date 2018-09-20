package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.domain.AnalyticsService
import hamburg.remme.tinygit.event.RepositoryClosedEvent
import hamburg.remme.tinygit.event.RepositoryOpenedEvent
import hamburg.remme.tinygit.event.RepositoryUpdatedEvent
import hamburg.remme.tinygit.system.git.Commit
import javafx.fxml.FXML
import javafx.scene.control.Label
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller
import java.io.File
import java.time.temporal.ChronoUnit

/**
 * Controller handling the stats bar on the right side.
 */
@Controller class StatsController(private val service: AnalyticsService) {

    @FXML private lateinit var commitCountLabel: Label
    @FXML private lateinit var authorCountLabel: Label
    @FXML private lateinit var ageLabel: Label

    /**
     * Handles a repository being opened.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryOpened(event: RepositoryOpenedEvent) {
        updateStats(event.directory)
    }

    /**
     * Handles a repository being updated.
     * @param event the event containing the repository directory.
     */
    @EventListener fun handleRepositoryUpdated(event: RepositoryUpdatedEvent) {
        updateStats(event.directory)
    }

    /**
     * Handles a repository being closed.
     * @param event the event.
     */
    @EventListener fun handleRepositoryClosed(event: RepositoryClosedEvent) {
        commitCountLabel.text = ""
        authorCountLabel.text = ""
        ageLabel.text = ""
    }

    private fun updateStats(directory: File) {
        commitCountLabel.text = service.count(directory).toString()
        authorCountLabel.text = service.countUnique(directory, Commit::authorEmail).toString()
        ageLabel.text = service.age(directory, ChronoUnit.MONTHS).toString()
    }

}
