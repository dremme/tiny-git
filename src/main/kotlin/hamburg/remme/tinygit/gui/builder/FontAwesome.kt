package hamburg.remme.tinygit.gui.builder

import javafx.scene.Node
import javafx.scene.text.Text

object FontAwesome {

    fun check() = icon('\uf00c')
    fun cloud() = icon('\uf0c2')
    fun cloudDownload() = icon('\uf381')
    fun cloudUpload() = icon('\uf382')
    fun codeFork() = icon('\uf126')
    fun cog() = icon('\uf013')
    fun cube() = icon('\uf1b2')
    fun cubes() = icon('\uf1b3')
    fun database() = icon('\uf1c0')
    fun desktop() = icon('\uf108')
    fun envelope() = icon('\uf0e0')
    fun exclamationTriangle() = icon('\uf071')
    fun folder() = icon('\uf07b')
    fun folderOpen() = icon('\uf07c')
    fun gavel() = icon('\uf0e3')
    fun githubAlt() = icon('\uf113', true)
    fun globe() = icon('\uf0ac')
    fun link() = icon('\uf0c1')
    fun list() = icon('\uf03a')
    fun minus() = icon('\uf068')
    fun pencil() = icon('\uf303')
    fun plus() = icon('\uf067')
    fun question() = icon('\uf128')
    fun questionCircle() = icon('\uf059')
    fun refresh() = icon('\uf021')
    fun search() = icon('\uf002')
    fun share() = icon('\uf064')
    fun spinner() = icon('\uf110')
    fun tag() = icon('\uf02b')
    fun tags() = icon('\uf02c')
    fun trash() = icon('\uf1f8')
    fun undo() = icon('\uf0e2')

    private fun icon(glyph: Char, brand: Boolean = false): Node {
        val icon = Text(glyph.toString()).addClass("icon")
        if (brand) icon.addClass("brand")
        return stackPane {
            minWidth = 14.0
            minHeight = 14.0
            +icon
        }
    }

}
