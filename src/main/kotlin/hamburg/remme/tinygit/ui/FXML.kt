package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.App
import hamburg.remme.tinygit.toURL
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import java.util.ResourceBundle

/**
 * Loads an FXML file as component.
 * @param path       the path the the FXML file.
 * @param controller the UI controller to use.
 * @param resources  the resource bundle used for translations.
 */
internal fun fxml(path: String, controller: Any, resources: ResourceBundle): Node {
    // A bug? is leaving the context class loader null after starting up JavaFX.
    // We have to manually set it with one we know is working for FXML loading.
    Thread.currentThread().contextClassLoader ?: setContextLoader()

    val fxmlLoader = FXMLLoader(path.toURL())
    fxmlLoader.resources = resources
    fxmlLoader.setController(controller)

    return fxmlLoader.load()
}

private fun setContextLoader() {
    Thread.currentThread().contextClassLoader = App::class.java.classLoader
}
