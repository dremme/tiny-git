package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.dateTimeFormat
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.fontSize
import hamburg.remme.tinygit.htmlEncode
import hamburg.remme.tinygit.htmlEncodeAll

/**
 * @todo: get style from JavaFX CSS
 */
class DetailsRenderer {

    fun render(commit: Commit?): String {
        //language=HTML
        if (commit == null) return """
            <html>
            <head>
                <style>
                    html, body {
                        background-color: #3c3f41;
                    }
                </style>
            </head>
            </html>
        """
        //language=HTML
        return """
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        width: 100%;
                        height: 100%;
                        font: ${fontSize}px "Roboto", sans-serif;
                        color: rgba(255, 255, 255, 0.9);
                        background-color: #263238;
                    }
                    table {
                        padding: 0;
                        border-spacing: 1em;
                        border-collapse: separate;
                        position: absolute;
                        min-width: 100%;
                        font-size: ${fontSize}px;
                    }
                    .label {
                        width: 1px;
                        white-space: nowrap;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <table>
                    <tr><td class="label">${I18N["commitDetails.commit"]}:</td><td>${commit.shortId} (${commit.id})</td></tr>
                    <tr><td class="label">${I18N["commitDetails.parents"]}:</td><td>${commit.shortParents.joinToString()}</td></tr>
                    <tr><td class="label">${I18N["commitDetails.author"]}:</td><td>${commit.author.htmlEncode()}</td></tr>
                    <tr><td class="label">${I18N["commitDetails.date"]}:</td><td>${commit.date.format(dateTimeFormat)}</td></tr>
                    <tr><td colspan="2"><br/>${commit.fullMessage.htmlEncodeAll().replace("\r?\n".toRegex(), "<br/>")}</td></tr>
                </table>
            </body>
            </html>
        """
    }

}
