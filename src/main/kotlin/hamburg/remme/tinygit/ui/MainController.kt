package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.domain.LogService
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.springframework.stereotype.Controller

@Controller class MainController(private val service: LogService) {

    @FXML private lateinit var repositoryPath: TextField
    @FXML private lateinit var commitCount: Label

    fun countCommits() {
        commitCount.text = "There are ${service.countAll()} commit(s)"
    }

}
