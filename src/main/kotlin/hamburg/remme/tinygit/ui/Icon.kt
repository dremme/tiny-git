package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.loadYaml
import javafx.beans.NamedArg
import javafx.scene.layout.StackPane
import javafx.scene.text.Text

/**
 * A glyph representing on of the Font Awesome 5 glyphs (https://fontawesome.com/icons).
 */
class Icon(@NamedArg("value") value: String) : StackPane() {

    private companion object {

        val FONT_AWESOME_5: Map<String, String> = "/font/fa-solid.yml".loadYaml()

    }

    init {
        styleClass += "icon-wrapper"

        val text = Text(getCodePoint(value).toChar().toString())
        text.styleClass += "icon"
        children += text
    }

    private fun getCodePoint(value: String): Int {
        return FONT_AWESOME_5[value]?.toInt(16) ?: throw IllegalArgumentException("$value is not a valid icon.")
    }

}
