package hamburg.remme.tinygit.gui.builder

import javafx.scene.control.ProgressBar

inline fun progressBar(block: ProgressBar.() -> Unit): ProgressBar {
    val bar = ProgressBar(-1.0)
    block.invoke(bar)
    return bar
}
