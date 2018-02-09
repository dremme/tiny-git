package hamburg.remme.tinygit.gui.builder

import javafx.animation.Interpolator
import javafx.animation.ScaleTransition
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.shape.Circle
import javafx.util.Duration

fun progressIndicator(size: Double): Node {
    return hbox {
        spacing = size / 3
        alignment = Pos.CENTER
        +Circle(size).addClass("progress-circle").attachAnimation(0.0)
        +Circle(size).addClass("progress-circle").attachAnimation(500.0)
        +Circle(size).addClass("progress-circle").attachAnimation(1000.0)
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
