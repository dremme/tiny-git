package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node

// TODO: return nothing
fun <T : Node> T.addClass(vararg styleClass: String): T {
    this.styleClass += styleClass
    return this
}

// TODO: return nothing
fun <T : Node> T.addStyle(style: String): T {
    this.style += style.let { if (!it.endsWith(";")) "$it;" else it }
    return this
}

// TODO: return nothing
fun <T : Node> T.flipX(): T {
    scaleX = -1.0
    return this
}

// TODO: return nothing
fun <T : Node> T.flipY(): T {
    scaleY = -1.0
    return this
}

// TODO: return nothing
fun <T : Node> T.flipXY() = flipX().flipY()
