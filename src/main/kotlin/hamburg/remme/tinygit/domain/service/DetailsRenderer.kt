package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.dateTimeFormat
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.htmlEncode
import hamburg.remme.tinygit.htmlEncodeAll

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
                        font: 13px "Roboto", sans-serif;
                        color: white;
                        background-color: #3c3f41;
                    }
                    table {
                        padding: 8px;
                        position: absolute;
                        min-width: 100%;
                        font-size: 13px;
                    }
                    .label {
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <table>
                    <tr><td class="label">${I18N["commitDetails.commit"]}:</td><td>${commit.id} [${commit.shortId}]</td></tr>
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
