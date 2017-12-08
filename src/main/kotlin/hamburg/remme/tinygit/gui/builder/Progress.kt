package hamburg.remme.tinygit.gui.builder

import javafx.animation.Interpolator
import javafx.animation.Transition
import javafx.scene.Node
import javafx.scene.control.ProgressBar
import javafx.util.Duration

private val spinAnimationStep = 8.0

inline fun progressBar(block: ProgressBar.() -> Unit): ProgressBar {
    val bar = ProgressBar(-1.0)
    block.invoke(bar)
    return bar
}

inline fun progressSpinner(block: Node.() -> Unit): Node {
    val indicator = FontAwesome.spinner()
    block.invoke(indicator)
    SpinAnimation(indicator, 2.0).play()
    return indicator
}

class SpinAnimation(private val node: Node, rate: Double = 1.0) : Transition(spinAnimationStep / rate) {

    init {
        cycleCount = -1
        cycleDuration = Duration(spinAnimationStep * rate * 1000.0)
        interpolator = Interpolator.DISCRETE
    }

    override fun interpolate(frac: Double) {
        node.rotate += 360.0 / spinAnimationStep
    }

}
