package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCombination

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

inline fun menuItem(block: MenuItem.() -> Unit): MenuItem {
    val item = MenuItem()
    block.invoke(item)
    return item
}

inline fun contextMenu(block: ContextMenuBuilder.() -> Unit): ContextMenu {
    val menu = ContextMenuBuilder()
    block.invoke(menu)
    return menu
}

class MenuBarBuilder : MenuBar() {

    operator fun Menu.unaryPlus() {
        menus.add(this)
    }

    operator fun ActionCollection.unaryPlus() {
        +menu {
            text = this@unaryPlus.text
            group.forEach { +it }
        }
    }

}

class MenuBuilder : Menu() {

    operator fun MenuItem.unaryPlus() {
        items.add(this)
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +SeparatorMenuItem()
        action.forEach {
            +menuItem {
                text = it.text
                graphic = it.icon.invoke()
                it.shortcut?.let { accelerator = KeyCombination.valueOf(it) }
                it.disable?.let { disableProperty().bind(it) }

                val handler = it.handler
                setOnAction { handler.invoke() }
            }
        }
    }

}

class ContextMenuBuilder : ContextMenu() {

    operator fun MenuItem.unaryPlus() {
        items.add(this)
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +SeparatorMenuItem()
        action.forEach {
            +menuItem {
                text = it.text
                graphic = it.icon.invoke()
                it.shortcut?.let { accelerator = KeyCombination.valueOf(it) }
                it.disable?.let { disableProperty().bind(it) }

                val handler = it.handler
                setOnAction { handler.invoke() }
            }
        }
    }

}
