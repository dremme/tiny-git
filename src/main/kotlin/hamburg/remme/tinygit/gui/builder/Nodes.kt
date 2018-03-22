package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.asResource
import javafx.beans.value.ObservableBooleanValue
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Tooltip
import javafx.util.Duration

fun <T : Node> T.addClass(vararg styleClass: String): T {
    this.styleClass += styleClass
    return this
}

fun <T : Node> T.addStyle(style: String): T {
    this.style += style.takeIf { it.endsWith(';') } ?: "$style;"
    return this
}

fun <T : Parent> T.addStylesheet(stylesheet: String): T {
    this.stylesheets += stylesheet.asResource()
    return this
}

fun <T : Node> T.flipX(): T {
    scaleX = -1.0
    return this
}

fun <T : Node> T.flipY(): T {
    scaleY = -1.0
    return this
}

fun <T : Node> T.flipXY() = flipX().flipY()

fun <T : Node> T.visibleWhen(observable: ObservableBooleanValue): T {
    visibleProperty().bind(observable)
    return this
}

fun <T : Node> T.managedWhen(observable: ObservableBooleanValue): T {
    managedProperty().bind(observable)
    return this
}

fun <T : Node> T.disabledWhen(observable: ObservableBooleanValue): T {
    disableProperty().bind(observable)
    return this
}

fun <T : Node> T.tooltip(text: String): T {
    val tooltip = Tooltip(text)
    tooltip.showDelay = Duration.ZERO
    tooltip.hideDelay = Duration.ZERO
    Tooltip.install(this, tooltip)
    return this
}
