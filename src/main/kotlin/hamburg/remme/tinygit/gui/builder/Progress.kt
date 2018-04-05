package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.fontSize
import javafx.animation.Interpolator
import javafx.animation.ScaleTransition
import javafx.scene.Node
import javafx.scene.shape.Circle
import javafx.util.Duration

private const val DEFAULT_STYLE_CLASS = "progress"
private const val CIRCLE_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__circle"

fun progressIndicator(size: Double): Node {
    return hbox {
        addClass(DEFAULT_STYLE_CLASS)
        +Circle(size * fontSize).addClass(CIRCLE_STYLE_CLASS).attachAnimation(0.0)
        +Circle(size * fontSize).addClass(CIRCLE_STYLE_CLASS).attachAnimation(500.0)
        +Circle(size * fontSize).addClass(CIRCLE_STYLE_CLASS).attachAnimation(1000.0)
    }
}

private fun Node.attachAnimation(delay: Double): Node {
    val transition = ScaleTransition(Duration(1000.0), this)
    transition.delay = Duration.millis(delay)
    transition.toX = 0.0
    transition.toY = 0.0
    transition.isAutoReverse = true
    transition.cycleCount = -1
    transition.interpolator = Interpolator.EASE_BOTH
    transition.play()
    return this
}
