package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asResource
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.link
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.stage.Window

class AboutDialog(window: Window) : Dialog(window, "About") {

    init {
        +DialogButton(DialogButton.CLOSE)

        header = "TinyGit ${javaClass.`package`.implementationVersion ?: ""}"
        graphic = Image("icon.png".asResource())
        content = grid(2) {
            addClass("about-view")

            val author = label {
                addClass("author")
                columnSpan(2)
                text = "Dennis Remme"
            }
            val link = link {
                text = "remme.hamburg"
                setOnAction { TinyGit.show("https://remme.hamburg") }
            }

            +listOf(author,
                    FontAwesome.envelope(), Label("dennis@remme.hamburg"),
                    FontAwesome.globe(), link)
        }
    }

}
