package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.isMac
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCombination

inline fun menuBar(block: MenuBarBuilder.() -> Unit): MenuBar {
    val menuBar = MenuBarBuilder()
    block(menuBar)
    return menuBar
}

inline fun menu(block: MenuBuilder.() -> Unit): Menu {
    val menu = MenuBuilder()
    block(menu)
    return menu
}

inline fun menuItem(block: MenuItemBuilder.() -> Unit): MenuItem {
    val item = MenuItemBuilder()
    block(item)
    return item
}

fun menuItem(action: Action): MenuItem {
    return menuItem {
        text = action.text
        icon = action.icon?.invoke()
        action.shortcut?.let { accelerator = KeyCombination.valueOf(it) }
        action.disabled?.let { disableProperty().bind(it) }
        setOnAction { action.handler() }
    }
}

inline fun contextMenu(block: ContextMenuBuilder.() -> Unit): ContextMenu {
    val menu = ContextMenuBuilder()
    block(menu)
    return menu
}

fun contextMenuItem(action: Action): MenuItem {
    return menuItem {
        text = action.text
        graphic = action.icon?.invoke()
        action.disabled?.let { disableProperty().bind(it) }
        setOnAction { action.handler() }
    }
}

/**
 * @todo: find icon solution for Mac OS
 */
class MenuItemBuilder : MenuItem() {

    var shortcut: String
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            accelerator = KeyCombination.valueOf(value)
        }
    var icon: Node?
        get() = graphic
        set(icon) {
            graphic = when {
                isMac -> null
                else -> icon
            }
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
