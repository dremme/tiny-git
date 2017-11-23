package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asResource
import hamburg.remme.tinygit.gui.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.event.EventHandler
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.GridPane
import javafx.stage.Window

class AboutDialog(window: Window) : Dialog(window, "About") {

    init {
        val ok = ButtonType("Done", ButtonBar.ButtonData.OK_DONE)

        var row = 0
        val content = GridPane().addClass("about-view")
        content.add(Label("Dennis Remme").addClass("author"), 0, row++, 2, 1)
        content.add(FontAwesome.envelope(), 0, row)
        content.add(Label("dennis@remme.hamburg"), 1, row++)
        content.add(FontAwesome.globe(), 0, row)
        content.add(Hyperlink("remme.hamburg").also {
            it.onAction = EventHandler { TinyGit.show("https://remme.hamburg") }
        }, 1, row)

        setHeader("TinyGit ${javaClass.`package`.implementationVersion ?: ""}")
        setIcon(Image("icon.png".asResource()))
        setContent(content)
        setButton(ok)
    }

}
