package hamburg.remme.tinygit

import javafx.stage.Window
import java.util.ResourceBundle

/**
 * The application context. Holding application related object like the primary [Window] and application state.
 */
class Context {

    /**
     * The primary window used by the app.
     */
    var window: Window by LateImmutable()

    /**
     * The primary resource bundle for translations.
     */
    var resources: ResourceBundle by LateImmutable()

}
