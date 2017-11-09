package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView

class FileDiffView : StackPane() {

    private val webView = WebView()

    init {
        webView.isContextMenuEnabled = false
        webView.prefWidth = 400.0
        webView.prefHeight = 300.0

        children += webView
        clearContent()
    }

    fun update(repository: LocalRepository, file: LocalFile) {
        setContent(when (file.status) {
            LocalFile.Status.CONFLICT -> LocalGit.diffCached(repository, file) // TODO: incorrect diff
            LocalFile.Status.ADDED -> LocalGit.diffCached(repository, file)
            LocalFile.Status.CHANGED -> LocalGit.diffCached(repository, file)
            LocalFile.Status.REMOVED -> LocalGit.diffCached(repository, file)
            LocalFile.Status.MODIFIED -> LocalGit.diff(repository, file)
            LocalFile.Status.MISSING -> LocalGit.diff(repository, file)
            LocalFile.Status.UNTRACKED -> LocalGit.diff(repository, file)
        }, file.resolve(repository))
    }

    fun update(repository: LocalRepository, file: LocalFile, id: String) {
        setContent(LocalGit.diff(repository, file, id), file.resolve(repository))
    }

    fun clear() {
        clearContent()
    }

    private fun clearContent() {
        //language=HTML
        webView.engine.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        width: 100%;
                        height: 100%;
                        font: 18px "Roboto", sans-serif;
                        color: #ccc;
                        background-color: #3c3f41;
                    }
                    body {
                        display: -webkit-flex;
                        display: flex;
                        -webkit-justify-content: center;
                        justify-content: center;
                        -webkit-align-items: center;
                        align-items: center;
                    }
                </style>
            </head>
            <body>
                Select a file to view its diff.
            </body>
            </html>
        """)
    }

    private fun setContent(diff: String, file: String) {
        //language=HTML
        webView.engine.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        font: 12px "Liberation Mono", monospace;
                        color: #ccc;
                        background-color: #3c3f41;
                    }
                    hr {
                        height: 1px;
                        background-color: #aaa;
                        border: none;
                    }
                    table {
                        position: absolute;
                        min-width: 100%;
                        font-size: 13px;
                    }
                    .image-box {
                        padding: 20px;
                    }
                    .image-box img {
                        width: 100%;
                        box-shadow: 0 2px 10px 2px rgba(0, 0, 0, 0.5);
                    }
                    .line-number {
                        padding: 3px 6px;
                        text-align: right;
                        color: rgba(255,255,255,0.6);
                        background-color: #535759;
                    }
                    .line-number.header {
                        padding: 6px 0;
                        background-color: #4e6e80;
                    }
                    .line-number.added {
                        background-color: #4e8054;
                    }
                    .line-number.removed {
                        background-color: #804e4e;
                    }
                    .code {
                        width: 100%;
                        white-space: nowrap;
                    }
                    .code.header {
                        color: #aaa;
                        background-color: #354b57;
                    }
                    .code.added {
                        background-color: #36593b;
                    }
                    .code.removed {
                        background-color: #593636;
                    }
                    .code.eof {
                        color: rgba(255,255,255,0.6);
                    }
                    .marker {
                        -webkit-user-select: none;
                        user-select: none;
                        margin-left: 4px;
                        padding: 0 2px;
                        color: rgba(255,255,255,0.45);
                        background-color: rgba(255,255,255,0.15);
                        border-radius: 2px;
                        font-size: 11px;
                    }
                </style>
            </head>
            <body>
                <table cellpadding="0" cellspacing="0">
                    ${format(diff, file)}
                </table>
            </body>
            </html>
        """)
    }

    private fun format(diff: String, file: String): String {
        if (diff.isBlank() || diff.matches(".*Binary files differ\\r?\\n?$".toRegex(RegexOption.DOT_MATCHES_ALL))) {
            val image: String
            if (file.toLowerCase().matches(".*\\.(png|jpe?g|gif)$".toRegex())) {
                //language=HTML
                image = """
                    <tr>
                        <td colspan="3"><div class="image-box"><img src="file://$file"></div></td>
                    </tr>
                """
            } else {
                image = ""
            }
            //language=HTML
            return """
                <tr>
                    <td class="line-number header">&nbsp;</td>
                    <td class="line-number header">&nbsp;</td>
                    <td class="code header">&nbsp;@@ No changes detected or binary file @@</td>
                </tr>
                $image
            """
        }
        val blocks = mutableListOf<DiffBlock>()
        var blockNumber = -1
        val numbers = arrayOf(0, 0)
        return diff.replace("\r\n", "\$CR$\n")
                .replace("\n", "\$LF$\n")
                .split("\\r?\\n".toRegex())
                .dropLast(1)
                .dropWhile { !it.isBlockHeader() }
                .onEach { if (it.isBlockHeader()) blocks += parseBlockHeader(it) }
                .map { it.replace("&", "&amp;") }
                .map { it.replace("<", "&lt;") }
                .map { it.replace(">", "&gt;") }
                .map { it.replace(" ", "&nbsp;") }
                .map {
                    if (it.isBlockHeader()) {
                        blockNumber++
                        numbers[0] = blocks[blockNumber].number1
                        numbers[1] = blocks[blockNumber].number2
                    }
                    formatLine(it, numbers, blocks[blockNumber])
                }
                .joinToString("")
    }

    private fun String.isBlockHeader() = this.startsWith("@@")

    private fun parseBlockHeader(line: String): DiffBlock {
        val match = ".*?(\\d+)(,\\d+)?.*?(\\d+)(,\\d+)?.*".toRegex().matchEntire(line)!!.groups
        return DiffBlock(
                match[1]?.value?.toInt() ?: 1,
                match[2]?.value?.substring(1)?.toInt() ?: 1,
                match[3]?.value?.toInt() ?: 1,
                match[4]?.value?.substring(1)?.toInt() ?: 1)
    }

    private fun formatLine(line: String, numbers: Array<Int>, block: DiffBlock): String {
        if (line.isBlockHeader()) {
            //language=HTML
            return """
                <tr>
                    <td class="line-number header">&nbsp;</td>
                    <td class="line-number header">&nbsp;</td>
                    <td class="code header">&nbsp;@@ -${block.number1},${block.length1} +${block.number2},${block.length2} @@</td>
                </tr>
            """
        }
        val code: String
        val codeClass: String
        val oldLineNumber: String
        val newLineNumber: String
        when {
            line.startsWith("+") -> {
                newLineNumber = numbers[1]++.toString()
                oldLineNumber = "&nbsp;"
                code = line.replaceMarkers()
                codeClass = "added"
            }
            line.startsWith("-") -> {
                newLineNumber = "&nbsp;"
                oldLineNumber = numbers[0]++.toString()
                code = line.replaceMarkers()
                codeClass = "removed"
            }
            line.startsWith("\\") -> {
                newLineNumber = "&nbsp;"
                oldLineNumber = "&nbsp;"
                code = line.stripMarkers()
                codeClass = "eof"
            }
            else -> {
                oldLineNumber = numbers[0]++.toString()
                newLineNumber = numbers[1]++.toString()
                code = line.stripMarkers()
                codeClass = "&nbsp;"
            }
        }
        //language=HTML
        return """
            <tr>
                <td class="line-number $codeClass">$oldLineNumber</td>
                <td class="line-number $codeClass">$newLineNumber</td>
                <td class="code $codeClass">$code</td>
            </tr>
        """
    }

    private fun String.replaceMarkers()
            = this.replace("\\\$CR\\$\\\$LF\\$$".toRegex(), "<span class=\"marker\">&#92;r&#92;n</span>")
            .replace("\\\$LF\\$$".toRegex(), "<span class=\"marker\">&#92;n</span>")

    private fun String.stripMarkers() = this.replace("(\\\$CR\\$)?\\\$LF\\$$".toRegex(), "")

    private class DiffBlock(val number1: Int, val length1: Int, val number2: Int, val length2: Int)

}
