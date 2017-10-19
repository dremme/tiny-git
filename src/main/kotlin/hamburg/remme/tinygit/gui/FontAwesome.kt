package hamburg.remme.tinygit.gui

import javafx.scene.control.Label

object FontAwesome {

    fun arrowCircleDown() = icon('\uf0ab')
    fun arrowCircleODown() = icon('\uf01a')
    fun arrowCircleUp() = icon('\uf0aa')
    fun arrowDown() = icon('\uf063')
    fun arrowUp() = icon('\uf062')
    fun cloud() = icon('\uf0c2')
    fun code() = icon('\uf121')
    fun codeFork() = icon('\uf126')
    fun check() = icon('\uf00c')
    fun database() = icon('\uf1c0')
    fun desktop() = icon('\uf108')
    fun download() = icon('\uf019')
    fun file() = icon('\uf15b')
    fun folderOpen() = icon('\uf07c')
    fun git() = icon('\uf1d3')
    fun cog() = icon('\uf013')
    fun list() = icon('\uf03a')
    fun minus() = icon('\uf068')
    fun minusSquare() = icon('\uf146')
    fun pencil() = icon('\uf040')
    fun pencilSquare() = icon('\uf14b')
    fun plus() = icon('\uf067')
    fun plusCircle() = icon('\uf055')
    fun plusSquare() = icon('\uf0fe')
    fun question() = icon('\uf128')
    fun questionCircle() = icon('\uf059')
    fun refresh() = icon('\uf021')
    fun tag() = icon('\uf02b')
    fun undo() = icon('\uf0e2')
    fun upload() = icon('\uf093')

    private fun icon(char: Char) = Label(char.toString()).also { it.styleClass += "icon" }

}
