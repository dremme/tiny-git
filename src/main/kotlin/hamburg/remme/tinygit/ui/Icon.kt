package hamburg.remme.tinygit.ui

import javafx.beans.NamedArg
import javafx.scene.layout.StackPane
import javafx.scene.text.Text

/**
 * A glyph representing on of the Font Awesome 5 glyphs (https://fontawesome.com/icons).
 */
class Icon(@NamedArg("value") value: String) : StackPane() {

    init {
        styleClass += "icon-wrapper"

        val text = Text(getCodePoint(value))
        text.styleClass += "icon"
        children += text
    }

    private fun getCodePoint(value: String): String {
        return when (value) {
            "calendar-alt" -> "\uf073"
            "user" -> "\uf007"
            else -> throw IllegalArgumentException("$value is not a valid Font Awesome 5 icon.")
        }
    }

}
