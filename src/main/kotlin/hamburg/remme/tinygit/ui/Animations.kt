package hamburg.remme.tinygit.ui

import javafx.animation.Animation
import javafx.animation.ScaleTransition
import javafx.animation.Timeline
import javafx.scene.Node
import javafx.util.Duration

/**
 * A convenience method for [ScaleTransition]. The cycle count is indefinite and the animation is auto reversing.
 * @param node     the target node. See [ScaleTransition.node].
 * @param toX      the target x-scale, default `0.0`. See [ScaleTransition.toX].
 * @param toY      the target x-scale, default `0.0`. See [ScaleTransition.toY].
 * @param duration the animation duration in seconds, default `1.0`. See [ScaleTransition.duration].
 * @param delay    the animation delay in seconds, default `0.0`. See [ScaleTransition.delay].
 */
internal fun scaleAnimation(node: Node,
                            toX: Double = 0.0,
                            toY: Double = 0.0,
                            duration: Double = 1.0,
                            delay: Double = 0.0): Animation {
    val animation = ScaleTransition()
    animation.node = node
    animation.cycleCount = Timeline.INDEFINITE
    animation.isAutoReverse = true
    animation.duration = Duration.seconds(duration)
    animation.delay = Duration.seconds(delay)
    animation.fromX = 1.0
    animation.fromY = 1.0
    animation.toX = toX
    animation.toY = toY
    return animation
}
