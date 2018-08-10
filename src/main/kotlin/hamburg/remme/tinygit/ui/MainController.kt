package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.domain.RepositoryService
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.springframework.stereotype.Controller
import java.io.File

@Controller class MainController(private val service: RepositoryService) {

    @FXML private lateinit var repositoryPath: TextField
    @FXML private lateinit var commitCount: Label

    fun countCommits() {
        val gitDir = File(repositoryPath.text)
        commitCount.text = "There are ${service.count(gitDir)} commit(s)"
    }

}
