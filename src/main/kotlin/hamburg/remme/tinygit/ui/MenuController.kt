package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
import javafx.stage.DirectoryChooser
import org.springframework.stereotype.Controller

@Controller class MenuController(private val context: Context) {

    fun onOpen() {
        val chooser = DirectoryChooser()
        chooser.showDialog(context.window)
    }

}
