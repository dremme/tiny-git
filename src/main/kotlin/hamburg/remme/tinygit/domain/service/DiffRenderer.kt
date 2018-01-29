package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.htmlEncodeAll

// TODO: implement word diff
// TODO: re-enable line numbers
class DiffRenderer {

    fun render(rawDiff: String): String {
        //language=HTML
        return """
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        font: 13px "Liberation Mono", monospace;
                        color: #ccc;
                        background-color: #3c3f41;
                    }
                    body {
                    }
                    .container {
                        position: absolute;
                        min-width: 100%;
                    }
                    .code {
                        padding: 4px 8px;
                        white-space: nowrap;
                    }
                    .header {
                        color: #aaa;
                        background-color: #354b57;
                    }
                    .added {
                        background-color: #36593b;
                    }
                    .removed {
                        background-color: #593636;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    ${rawDiff.lines().fold(StringBuilder()) { builder, it -> builder.parseLine(it) }}
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun StringBuilder.parseLine(raw: String): StringBuilder {
        when {
            raw.startsWith("+++") || raw.startsWith("---") -> return this
            raw.isBlank() -> appendln(div(" ".htmlEncodeAll(), ""))
            raw.startsWith("* Unmerged path") -> appendln(divH("@@ Unmerged path @@"))
            raw.startsWith("Binary files ") -> appendln(divH("@@ Binary file @@"))
            raw.startsWith('@') -> appendln(divH(raw.substring(0, raw.indexOfLast { it == '@' } + 1).htmlEncodeAll()))
            raw.startsWith('+') -> appendln(divP(raw.sanitize()))
            raw.startsWith('-') -> appendln(divM(raw.sanitize()))
            raw.startsWith(' ') -> appendln(div(raw.sanitize(), ""))
        }
        return this
    }

    private fun String.sanitize() = (takeIf { isNotBlank() } ?: " ").htmlEncodeAll()

    private fun divM(text: String) = div(text, "removed")

    private fun divP(text: String) = div(text, "added")

    private fun divH(text: String) = div(text, "header")

    // language=HTML
    private fun div(text: String, clazz: String) = "<div class=\"code $clazz\">$text</div>"

}
