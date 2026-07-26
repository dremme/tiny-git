package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.service.DiffService
import hamburg.remme.tinygit.gui.builder.VBoxBuilder
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.listCell
import hamburg.remme.tinygit.gui.builder.listCellFactory
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.webView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableObjectValue
import javafx.scene.control.ComboBox
import javafx.scene.layout.Priority
import javafx.scene.web.WebEngine

//language=HTML
private const val EMPTY_DIFF = """
        <html>
        <head>
            <style>
                html, body {
                    background-color: #263238;
                }
            </style>
        </head>
        </html>
    """

/**
 * Rendering a file's diff between certain commits or the working copy.
 * Relies heavily on the [DiffService] and its renderer.
 *
 * Also contains a [ComboBox] to control the number of context lines for the shown diff.
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━┯━━━━━━━━━━━━━━━┓
 *   ┃            │ Context Lines ┃
 *   ┠────────────┴───────────────┨
 *   ┃  @@ -1,1 +1,1 @@           ┃
 *   ┃ +foo                       ┃
 *   ┃ -bar                       ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @todo: get style from JavaFX CSS
 *
 * @see DiffService
 */
class FileDiffView(
    private val file: ObservableObjectValue<File?>,
    private val commit: ObservableObjectValue<Commit?> = SimpleObjectProperty(),
) : VBoxBuilder() {
    private val service = TinyGit.get<DiffService>()
    private val contextLines: ComboBox<Int>
    private val engine: WebEngine
    private var diff = EMPTY_DIFF

    init {
        contextLines =
            comboBox {
                items.addAll(0, 1, 3, 6, 12, 25, 50, 100)
                buttonCell = listCell<Int> { text = I18N["fileDiff.lines", it ?: 0] }
                cellFactory = listCellFactory<Int> { text = I18N["fileDiff.lines", it ?: 0] }
                value = 3
                valueProperty().addListener { _ -> update() }
            }
        +toolBar {
            addSpacer()
            +contextLines
        }

        val webView =
            webView {
                vgrow(Priority.ALWAYS)
                isContextMenuEnabled = false
                prefWidth = 400.0
                prefHeight = 300.0
                engine.loadContent(diff)
            }
        engine = webView.engine
        +webView

        // React to selection and commit; also load the value that is already set.
        file.addListener { _, _, _ -> update() }
        commit.addListener { _, _, _ -> update() }
        // WebView can drop the first loadContent before it is attached to a scene.
        sceneProperty().addListener { _, _, scene -> if (scene != null) update() }
        update()
    }

    fun refresh() = update()

    private fun update() {
        val selectedFile = file.get()
        if (selectedFile == null) {
            show(EMPTY_DIFF)
            return
        }
        val selectedCommit = commit.get()
        val lines = contextLines.value ?: 3
        val newDiff =
            if (selectedCommit != null) {
                service.diff(selectedFile, selectedCommit, lines)
            } else {
                service.diff(selectedFile, lines)
            }
        show(newDiff)
    }

    private fun show(content: String) {
        if (content == diff && engine.document != null) return
        diff = content
        engine.loadContent(content)
    }
}
