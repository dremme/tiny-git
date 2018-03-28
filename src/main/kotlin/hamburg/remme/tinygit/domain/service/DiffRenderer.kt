package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.fontSize
import hamburg.remme.tinygit.htmlEncodeAll

// TODO: implement word diff
class DiffRenderer {

    fun render(rawDiff: String): String {
        val lineBuilder = rawDiff.lines().fold(LineBuilder()) { builder, it -> builder.parse(it) }
        //language=HTML
        return """
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        font: ${fontSize}px "Liberation Mono", monospace;
                        color: rgba(255, 255, 255, 0.9);
                        background-color: #37474F;
                    }
                    body {
                    }
                    .container {
                        position: absolute;
                        min-width: 100%;
                    }
                    .c {
                        padding: 0.5em 1em;
                        white-space: nowrap;
                    }
                    .c > div {
                        display: inline-block;
                    }
                    .h {
                        color: rgba(255, 255, 255, 0.8);
                        background-color: #546E7A;
                    }
                    .a {
                        background-color: #426643;
                    }
                    .r {
                        background-color: #664242;
                    }
                    .l-num,
                    .r-num {
                        padding: 0 0.5em;
                        width: ${lineBuilder.numWidth + 0.5}em;
                        text-align: right;
                    }
                </style>
            </head>
            <body>
                <div class="container">$lineBuilder</div>
            </body>
            </html>
        """
    }

    private class LineBuilder {

        val numWidth get() = Math.log10(maxNum.toDouble()).toInt()
        private val builder = StringBuilder()
        private val numRegex = "^@@@? [-+](\\d+)(,\\d+)? [-+](\\d+)(,\\d+)?.* @@@?.*".toRegex()
        //language=HTML
        private val emptyLeft = "<div class=\"l-num\"></div>"
        //language=HTML
        private val emptyRight = "<div class=\"r-num\"></div>"
        private var leftNum = 0
        private var rightNum = 0
        private var maxNum = 0

        fun parse(line: String): LineBuilder {
            when {
                line.startsWith("+++") -> return this
                line.startsWith("---") -> return this
                line.startsWith("* Unmerged path") -> parseInfo("@@ ${I18N["fileDiff.unmerged"]} @@")
                line.startsWith("Binary files") -> parseInfo("@@ ${I18N["fileDiff.binary"]} @@")
                line.startsWith("warning: LF will be replaced") -> parseInfo("@@ ${I18N["fileDiff.lineEndings"]} @@")
                line.startsWith("warning: CRLF will be replaced") -> parseInfo("@@ ${I18N["fileDiff.lineEndings"]} @@")
                line.startsWith('@') -> parseHead(line)
                line.startsWith('+') -> parseLineAdded(line)
                line.startsWith('-') -> parseLineRemoved(line)
                line.startsWith(' ') -> parseLine(line)
                line.startsWith('\\') -> parseEof(line)
            }
            maxNum = Math.max(maxNum, Math.max(leftNum, rightNum))
            return this
        }

        override fun toString(): String {
            if (builder.toString().isBlank()) parseInfo("@@ ${I18N["fileDiff.noChanges"]} @@")
            return builder.toString()
        }

        private fun parseInfo(line: String) {
            //language=HTML
            builder.appendln("<div class=\"c h\">$emptyLeft$emptyRight<div>${line.htmlEncodeAll()}</div></div>")
        }

        private fun parseHead(line: String) {
            leftNum = numRegex.matchEntire(line)!!.groupValues[1].toInt()
            rightNum = numRegex.matchEntire(line)!!.groupValues[3].toInt()
            val header = line.substring(0, line.indexOfLast { it == '@' } + 1).htmlEncodeAll()
            //language=HTML
            builder.appendln("<div class=\"c h\">$emptyLeft$emptyRight<div>$header</div></div>")
        }

        private fun parseLine(line: String) {
            //language=HTML
            builder.appendln("<div class=\"c\">${left()}${right()}<div>${line.sanitize()}</div></div>")
            leftNum++
            rightNum++
        }

        private fun parseEof(line: String) {
            //language=HTML
            builder.appendln("<div class=\"c\">$emptyLeft$emptyRight<div>${line.sanitize()}</div></div>")
        }

        private fun parseLineAdded(line: String) {
            //language=HTML
            builder.appendln("<div class=\"c a\">$emptyLeft${right()}<div>${line.sanitize()}</div></div>")
            rightNum++
        }

        private fun parseLineRemoved(line: String) {
            //language=HTML
            builder.appendln("<div class=\"c r\">${left()}$emptyRight<div>${line.sanitize()}</div></div>")
            leftNum++
        }

        //language=HTML
        private fun left() = "<div class=\"l-num\">$leftNum</div>"

        //language=HTML
        private fun right() = "<div class=\"r-num\">$rightNum</div>"

        private fun String.sanitize() = (takeIf { isNotBlank() } ?: " ").htmlEncodeAll()

    }

}
