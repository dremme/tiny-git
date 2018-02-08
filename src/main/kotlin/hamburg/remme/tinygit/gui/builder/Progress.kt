package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.service.TaskExecutor
import javafx.animation.Interpolator
import javafx.animation.ScaleTransition
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ProgressBar
import javafx.scene.layout.StackPane
import javafx.scene.shape.Circle
import javafx.util.Duration

inline fun progressBar(block: ProgressBar.() -> Unit): ProgressBar {
    val bar = ProgressBar(-1.0)
    block.invoke(bar)
    return bar
}

inline fun progressPane(block: ProgressPaneBuilder.() -> Unit): ProgressPane {
    val pane = ProgressPaneBuilder()
    block.invoke(pane)
    return pane
}

fun progressIndicator(): Node {
    return hbox {
        spacing = 16.0
        alignment = Pos.CENTER
        +Circle(48.0).addClass("progress-circle").attachAnimation(0.0)
        +Circle(48.0).addClass("progress-circle").attachAnimation(500.0)
        +Circle(48.0).addClass("progress-circle").attachAnimation(1000.0)
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

open class ProgressPane : StackPane(), TaskExecutor {

    private val progress = progressBar {
        addClass("progress-pane-bar")
        alignment(Pos.TOP_CENTER)
        maxWidth = Double.MAX_VALUE
        isVisible = false
    }

    init {
        children.add(progress)
    }

    override fun execute(task: Task<*>) {
        task.setOnSucceeded { hideProgress() }
        task.setOnCancelled { hideProgress() }
        task.setOnFailed { hideProgress() }
        showProgress()
        TinyGit.execute(task)
    }

    private fun showProgress() {
        progress.isVisible = true
    }

    private fun hideProgress() {
        progress.isVisible = false
    }

}

class ProgressPaneBuilder : ProgressPane() {

    operator fun Node.unaryPlus() {
        children.add(children.size - 1, this)
    }

}
