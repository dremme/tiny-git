package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asResource
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.link
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.stage.Window

class AboutDialog(window: Window) : Dialog(window, "About") {

    init {
        setHeader("TinyGit ${javaClass.`package`.implementationVersion ?: ""}")
        setIcon(Image("icon.png".asResource()))
        setButton(ButtonType("Done", ButtonBar.ButtonData.OK_DONE))
        setContent(grid {
            addClass("about-view")
            addRow(label {
                addClass("author")
                columnSpan(2)
                text = "Dennis Remme"
            })
            addRow(FontAwesome.envelope(), Label("dennis@remme.hamburg"))
            addRow(FontAwesome.globe(), link {
                text = "remme.hamburg"
                setOnAction { TinyGit.show("https://remme.hamburg") }
            })
        })
    }

}
