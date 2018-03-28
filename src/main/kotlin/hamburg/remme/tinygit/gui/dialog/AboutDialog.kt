package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
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

private const val DEFAULT_STYLE_CLASS = "about-dialog"
private const val AUTHOR_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__author"

class AboutDialog(window: Window) : Dialog<Unit>(window, I18N["dialog.about.title"]) {

    init {
        +DialogButton(DialogButton.CLOSE)

        header = "TinyGit ${javaClass.`package`.implementationVersion ?: ""}"
        image = Image("icon.png".asResource())
        content = grid(2) {
            addClass(DEFAULT_STYLE_CLASS)

            val author = label {
                addClass(AUTHOR_STYLE_CLASS)
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
