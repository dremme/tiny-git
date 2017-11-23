package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStyle
import javafx.scene.Node
import javafx.scene.layout.StackPane
import javafx.scene.text.Text

object FontAwesome {

    fun check(color: String? = null) = icon('\uf00c', color)
    fun cloud(color: String? = null) = icon('\uf0c2', color)
    fun cloudDownload(color: String? = null) = icon('\uf0ed', color)
    fun cloudUpload(color: String? = null) = icon('\uf0ee', color)
    fun codeFork(color: String? = null) = icon('\uf126', color)
    fun cog(color: String? = null) = icon('\uf013', color)
    fun cube(color: String? = null) = icon('\uf1b2', color)
    fun cubes(color: String? = null) = icon('\uf1b3', color)
    fun database(color: String? = null) = icon('\uf1c0', color)
    fun desktop(color: String? = null) = icon('\uf108', color)
    fun envelope(color: String? = null) = icon('\uf0e0', color)
    fun exclamationTriangle(color: String? = null) = icon('\uf071', color)
    fun folderOpen(color: String? = null) = icon('\uf07c', color)
    fun gavel(color: String? = null) = icon('\uf0e3', color)
    fun githubAlt(color: String? = null) = icon('\uf113', color)
    fun globe(color: String? = null) = icon('\uf0ac', color)
    fun list(color: String? = null) = icon('\uf03a', color)
    fun minus(color: String? = null) = icon('\uf068', color)
    fun pencil(color: String? = null) = icon('\uf040', color)
    fun plus(color: String? = null) = icon('\uf067', color)
    fun question(color: String? = null) = icon('\uf128', color)
    fun questionCircle(color: String? = null) = icon('\uf059', color)
    fun refresh(color: String? = null) = icon('\uf021', color)
    fun tag(color: String? = null) = icon('\uf02b', color)
    fun tags(color: String? = null) = icon('\uf02c', color)
    fun undo(color: String? = null) = icon('\uf0e2', color)

    private fun icon(glyph: Char, color: String? = null): Node {
        val icon = Text(glyph.toString()).addClass("icon")
        color?.let { icon.addStyle("-fx-fill:$it") }
        return StackPane(icon).also {
            it.minWidth = 14.0
            it.minHeight = 14.0
        }
    }

}
