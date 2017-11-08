package hamburg.remme.tinygit.gui

import javafx.scene.Node
import javafx.scene.text.Text

object FontAwesome {

    fun CHECK(color: String? = null) = icon('\uf00c', color)
    fun CLOUD(color: String? = null) = icon('\uf0c2', color)
    fun CLOUD_DOWNLOAD(color: String? = null) = icon('\uf0ed', color)
    fun CLOUD_UPLOAD(color: String? = null) = icon('\uf0ee', color)
    fun CODE_FORK(color: String? = null) = icon('\uf126', color)
    fun COG(color: String? = null) = icon('\uf013', color)
    fun DATABASE(color: String? = null) = icon('\uf1c0', color)
    fun DESKTOP(color: String? = null) = icon('\uf108', color)
    fun DOWNLOAD(color: String? = null) = icon('\uf019', color)
    fun EXCLAMATION_TRIANGLE(color: String? = null) = icon('\uf071', color)
    fun FOLDER_OPEN(color: String? = null) = icon('\uf07c', color)
    fun LIST(color: String? = null) = icon('\uf03a', color)
    fun MINUS(color: String? = null) = icon('\uf068', color)
    fun PENCIL(color: String? = null) = icon('\uf040', color)
    fun PLUS(color: String? = null) = icon('\uf067', color)
    fun QUESTION(color: String? = null) = icon('\uf128', color)
    fun QUESTION_CIRCLE(color: String? = null) = icon('\uf059', color)
    fun REFRESH(color: String? = null) = icon('\uf021', color)
    fun TAG(color: String? = null) = icon('\uf02b', color)
    fun UNDO(color: String? = null) = icon('\uf0e2', color)
    fun UPLOAD(color: String? = null) = icon('\uf093', color)

    private fun icon(glyph: Char, color: String? = null): Node {
        val icon = Text(glyph.toString())
        icon.styleClass += "icon"
        color?.let { icon.style = "-fx-fill:$it;" }
        return icon
    }

}
