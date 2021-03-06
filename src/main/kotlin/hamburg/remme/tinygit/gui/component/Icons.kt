package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.stackPane
import javafx.scene.Node
import javafx.scene.text.Text

/**
 * A list of icon factories creating [FontAwesome 5](https://fontawesome.com) icons.
 */
object Icons {

    fun arrowAltCircleDown() = icon('\uf358')
    fun arrowAltCircleUp() = icon('\uf35b')
    fun calendar() = icon('\uf073')
    fun chartPie() = icon('\uf200')
    fun check() = icon('\uf00c')
    fun clone() = icon('\uf24d')
    fun cloud() = icon('\uf0c2')
    fun cloudDownload() = icon('\uf381')
    fun cloudUpload() = icon('\uf382')
    fun codeFork() = icon('\uf126')
    fun coffee() = icon('\uf0f4')
    fun cog() = icon('\uf013')
    fun cube() = icon('\uf1b2')
    fun cubes() = icon('\uf1b3')
    fun envelope() = icon('\uf0e0')
    fun eraser() = icon('\uf12d')
    fun exclamationTriangle() = icon('\uf071')
    fun file() = icon('\uf15b')
    fun folder() = icon('\uf07b')
    fun folderOpen() = icon('\uf07c')
    fun forward() = icon('\uf04e')
    fun gavel() = icon('\uf0e3')
    fun github() = icon('\uf113', true)
    fun globe() = icon('\uf0ac')
    fun hdd() = icon('\uf0a0')
    fun levelUp() = icon('\uf3bf')
    fun list() = icon('\uf022')
    fun locationArrow() = icon('\uf124')
    fun minus() = icon('\uf068')
    fun pencil() = icon('\uf303')
    fun plus() = icon('\uf067')
    fun question() = icon('\uf128')
    fun questionCircle() = icon('\uf059')
    fun refresh() = icon('\uf021')
    fun search() = icon('\uf002')
    fun share() = icon('\uf064')
    fun signOut() = icon('\uf2f5')
    fun tag() = icon('\uf02b')
    fun tags() = icon('\uf02c')
    fun terminal() = icon('\uf120')
    fun timesCircle() = icon('\uf057')
    fun trash() = icon('\uf1f8')
    fun undo() = icon('\uf0e2')
    fun user() = icon('\uf007')

    private fun icon(glyph: Char, brand: Boolean = false): Node {
        val icon = Text(glyph.toString()).addClass("icon")
        if (brand) icon.addClass("brand")
        return stackPane {
            addClass("icon-wrapper")
            +icon
        }
    }

}
