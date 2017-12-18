package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.gui.builder.Icons
import javafx.scene.control.Tab

class StatsView : Tab() {

    init {
        text = "Stats (Soon!)"
        graphic = Icons.chartPie()
        isClosable = false
        isDisable = true
        // TODO
    }

}
