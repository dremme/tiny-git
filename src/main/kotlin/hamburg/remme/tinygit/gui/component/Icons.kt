package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.stackPane
import javafx.scene.Node
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text

/**
 * Icon factories using [Font Awesome 7 Free](https://fontawesome.com) glyph fonts.
 *
 * Solid icons use family `Font Awesome 7 Free` (weight 900).
 * Brand icons use family `Font Awesome 7 Brands`.
 */
object Icons {
    fun arrowAltCircleDown() = icon('\uf358')

    fun arrowAltCircleUp() = icon('\uf35b')

    fun box() = icon('\uf466')

    fun boxesStacked() = icon('\uf468')

    fun boxOpen() = icon('\uf49e')

    fun bug() = icon('\uf188')

    fun calendar() = icon('\uf133')

    fun chartPie() = icon('\uf200')

    fun check() = icon('\uf00c')

    fun clone() = icon('\uf24d')

    fun cloud() = icon('\uf0c2')

    fun cloudDownload() = icon('\uf0ed')

    fun cloudUpload() = icon('\uf0ee')

    fun codeBranch() = icon('\uf126')

    fun codeCommit() = icon('\uf386')

    fun codeCompare() = icon('\ue13a')

    fun codeFork() = icon('\ue13b')

    fun codeMerge() = icon('\uf387')

    fun codePullRequest() = icon('\ue13c')

    fun coffee() = icon('\uf7b6')

    fun cog() = icon('\uf013')

    fun cube() = icon('\uf1b2')

    fun cubes() = icon('\uf1b3')

    fun download() = icon('\uf019')

    fun envelope() = icon('\uf0e0')

    fun eraser() = icon('\uf12d')

    fun exclamationTriangle() = icon('\uf071')

    fun file() = icon('\uf15b')

    fun folder() = icon('\uf07b')

    fun folderOpen() = icon('\uf07c')

    fun folderPlus() = icon('\uf65e')

    fun forward() = icon('\uf04e')

    fun gavel() = icon('\uf0e3')

    fun github() = icon('\uf09b', brand = true)

    fun globe() = icon('\uf0ac')

    fun hdd() = icon('\uf0a0')

    fun levelUp() = icon('\uf3bf')

    fun alignLeft() = icon('\uf036')

    fun list() = icon('\uf03a')

    fun listUl() = icon('\uf0ca')

    fun locationArrow() = icon('\uf124')

    fun minus() = icon('\uf068')

    fun pencil() = icon('\uf304')

    fun plus() = icon('\u002b')

    fun question() = icon('\u003f')

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

    private fun icon(
        glyph: Char,
        brand: Boolean = false,
    ): Node {
        val icon = Text(glyph.toString()).addClass("icon")
        icon.fontSmoothingType = FontSmoothingType.GRAY
        if (brand) icon.addClass("brand")
        return stackPane {
            addClass("icon-wrapper")
            +icon
        }
    }
}
