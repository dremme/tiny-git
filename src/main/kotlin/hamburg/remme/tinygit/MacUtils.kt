package hamburg.remme.tinygit

import de.codecentric.centerdevice.MenuToolkit
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.menu
import hamburg.remme.tinygit.gui.builder.menuItem
import javafx.beans.binding.Bindings
import javafx.scene.control.SeparatorMenuItem
import javafx.stage.Stage
import java.util.concurrent.Callable

/**
 * Initializes and sets the Mac OS application menu.
 */
fun initMacApp(preferences: Action) {
    val toolkit = MenuToolkit.toolkit()
    toolkit.setApplicationMenu(menu {
        text = "TinyGit"
        +menuItem(preferences)
        +SeparatorMenuItem()
        +toolkit.createHideMenuItem("TinyGit")
        +toolkit.createHideOthersMenuItem()
        +toolkit.createUnhideAllMenuItem()
        +SeparatorMenuItem()
        +toolkit.createQuitMenuItem("TinyGit")
    })
}

/**
 * Creates Mac OS window actions zoom, minimize and fullscreen for the given [window].
 */
fun createMacWindow(window: Stage) = menu {
    text = I18N["menuBar.window"]
    +createMacMinimize(window)
    +createMacZoom(window)
    +createMacFullscreen(window)
}

private fun createMacZoom(window: Stage) = menuItem {
    shortcut = "Shortcut+Ctrl+Z"
    text = I18N["menu.zoom"]
    disableProperty().bind(window.fullScreenProperty())
    setOnAction { window.isMaximized = true }
}

private fun createMacMinimize(window: Stage) = menuItem {
    shortcut = "Shortcut+M"
    text = I18N["menu.minimize"]
    disableProperty().bind(window.fullScreenProperty())
    setOnAction { window.isIconified = true }
}

private fun createMacFullscreen(window: Stage) = menuItem {
    shortcut = "Shortcut+Ctrl+F"
    textProperty().bind(Bindings.createStringBinding(
            Callable { if (window.isFullScreen) I18N["menu.exitFullscreen"] else I18N["menu.enterFullscreen"] },
            window.fullScreenProperty()))
    setOnAction { window.isFullScreen = !window.isFullScreen }
}
