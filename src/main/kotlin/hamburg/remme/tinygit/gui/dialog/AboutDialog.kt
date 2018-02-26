package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asResource
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.link
import hamburg.remme.tinygit.gui.component.Icons
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.stage.Window

// TODO: should be wider
class AboutDialog(window: Window) : Dialog<Unit>(window, "About") {

    init {
        +DialogButton(DialogButton.CLOSE)

        header = "TinyGit ${javaClass.`package`.implementationVersion ?: ""}"
        image = Image("icon-small.png".asResource())
        content = grid(2) {
            addClass("about-view")

            val author = label {
                addClass("author")
                columnSpan(2)
                +"Dennis Remme"
            }
            val link = link {
                text = "remme.hamburg"
                setOnAction { TinyGit.showDocument("https://remme.hamburg") }
            }

            +listOf(author,
                    Icons.envelope(), Label("dennis@remme.hamburg"),
                    Icons.globe(), link)
        }
    }

}
