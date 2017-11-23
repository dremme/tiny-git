package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.gui.ActionCollection
import hamburg.remme.tinygit.gui.ActionGroup
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
    val menu = MenuItem()
    block.invoke(menu)
    return menu
}

open class MenuBarBuilder : MenuBar() {

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

open class MenuBuilder : Menu() {

    operator fun MenuItem.unaryPlus() {
        items.add(this)
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +SeparatorMenuItem()
        action.forEach {
            +menuItem {
                text = it.text
                graphic = it.icon.invoke()
                onAction = it.action
                it.shortcut?.let { accelerator = KeyCombination.valueOf(it) }
                it.disable?.let { disableProperty().bind(it) }
            }
        }
    }

}
