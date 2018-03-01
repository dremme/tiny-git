package hamburg.remme.tinygit.gui.builder

import com.sun.javafx.PlatformUtil
import hamburg.remme.tinygit.asResource
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCombination
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle

inline fun menuBar(block: MenuBarBuilder.() -> Unit): MenuBar {
    val menuBar = MenuBarBuilder()
    block.invoke(menuBar)
    return menuBar
}

inline fun menu(block: MenuBuilder.() -> Unit): Menu {
    val menu = MenuBuilder()
    block.invoke(menu)
    return menu
}

inline fun menuItem(block: MenuItemBuilder.() -> Unit): MenuItem {
    val item = MenuItemBuilder()
    block.invoke(item)
    return item
}

fun menuItem(action: Action): MenuItem {
    return menuItem {
        text = action.text
        icon = action.icon?.invoke()
        action.shortcut?.let { accelerator = KeyCombination.valueOf(it) }
        action.disable?.let { disableProperty().bind(it) }
        setOnAction { action.handler.invoke() }
    }
}

inline fun contextMenu(block: ContextMenuBuilder.() -> Unit): ContextMenu {
    val menu = ContextMenuBuilder()
    block.invoke(menu)
    return menu
}

fun contextMenuItem(action: Action): MenuItem {
    return menuItem {
        text = action.text
        graphic = action.icon?.invoke()
        action.disable?.let { disableProperty().bind(it) }
        setOnAction { action.handler.invoke() }
    }
}

class MenuItemBuilder : MenuItem() {

    var icon: Node?
        get() = graphic
        set(icon) = when {
            icon == null -> graphic = null
            PlatformUtil.isMac() -> Platform.runLater {
                val offscreenScene = Scene(StackPane((icon)).addClass("platform-parent"))
                offscreenScene.stylesheets += "platform-icons.css".asResource()
                offscreenScene.fill = Color.TRANSPARENT
                val offscreenStage = Stage()
                offscreenStage.scene = offscreenScene
                offscreenStage.initStyle(StageStyle.UNDECORATED)
                graphic = ImageView(offscreenScene.snapshot(null))
            }
            else -> graphic = icon
        }

}

class MenuBuilder : Menu() {

    operator fun MenuItem.unaryPlus() {
        items += this
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +SeparatorMenuItem()
        action.forEach { +menuItem(it) }
    }

}

class ContextMenuBuilder : ContextMenu() {

    operator fun MenuItem.unaryPlus() {
        items += this
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +SeparatorMenuItem()
        action.forEach { +contextMenuItem(it) }
    }

}

class MenuBarBuilder : MenuBar() {

    operator fun Menu.unaryPlus() {
        menus += this
    }

    operator fun ActionCollection.unaryPlus() {
        +menu {
            text = this@unaryPlus.text
            group.forEach { +it }
        }
    }

}
