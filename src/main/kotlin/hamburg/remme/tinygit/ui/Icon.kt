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

        val FONT_AWESOME_5_SOLID: Map<String, String> = "/font/fa-solid.yml".loadYaml()
        val FONT_AWESOME_5_BRAND: Map<String, String> = "/font/fa-brand.yml".loadYaml()

    }

    init {
        styleClass += "icon-wrapper"

        val text = Text(getCodePoint(value).toChar().toString())
        text.styleClass += "icon"
        children += text
    }

    private fun getCodePoint(value: String): Int {
        // Try for solid icons
        var s = FONT_AWESOME_5_SOLID[value]
        if (s == null) {
            // Try for brand icons
            s = FONT_AWESOME_5_BRAND[value]
            if (s != null) styleClass += "icon-wrapper--brand"
        }
        return s?.toInt(16) ?: throw IllegalArgumentException("$value is not a valid icon.")
    }

}
