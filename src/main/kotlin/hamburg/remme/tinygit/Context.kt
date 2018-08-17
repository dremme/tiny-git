package hamburg.remme.tinygit

import javafx.stage.Window

/**
 * The application context. Holding application related object like the primary [Window] and application state.
 */
class Context {

    /**
     * The primary window used by the app.
     */
    var window: Window by LateImmutable()

}
