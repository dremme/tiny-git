package hamburg.remme.tinygit.ui.list

import hamburg.remme.tinygit.App
import hamburg.remme.tinygit.toURL
import javafx.fxml.FXMLLoader
import javafx.scene.control.ListCell
import java.util.ResourceBundle

/**
 * A list cell that loads its content to the [graphic] from a FXML file.
 * @todo: the FXML loading can be in a more general place
 */
open class FXMLListCell<T>(fxmlPath: String, resources: ResourceBundle) : ListCell<T>() {

    init {
        // A bug? is leaving the context class loader null after starting up JavaFX.
        // We have to manually set it with one we know is working for FXML loading.
        Thread.currentThread().contextClassLoader ?: setContextLoader()

        val fxmlLoader = FXMLLoader(fxmlPath.toURL())
        fxmlLoader.resources = resources

        @Suppress("LeakingThis") // the cell is always the controller
        fxmlLoader.setController(this)

        graphic = fxmlLoader.load()
    }

    private fun setContextLoader() {
        Thread.currentThread().contextClassLoader = App::class.java.classLoader
    }

}
