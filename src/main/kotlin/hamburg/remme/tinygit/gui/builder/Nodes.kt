package hamburg.remme.tinygit.gui.builder

import javafx.beans.value.ObservableBooleanValue
import javafx.scene.Node
import javafx.scene.control.Tooltip
import javafx.util.Duration

fun <T : Node> T.addClass(vararg styleClass: String): T {
    this.styleClass += styleClass
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
    tooltip.showDelay = Duration.millis(250.0)
    Tooltip.install(this, tooltip)
    return this
}
