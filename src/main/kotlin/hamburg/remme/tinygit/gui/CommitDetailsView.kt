package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.gui.builder.SplitPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.builder.webView
import javafx.beans.binding.Bindings
import javafx.scene.layout.Priority

private const val DEFAULT_STYLE_CLASS = "commit-details-view"
private const val CONTENT_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__content"
private const val OVERLAY_STYLE_CLASS = "overlay"

/**
 * Showing details for a specific [hamburg.remme.tinygit.domain.Commit].
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━┓
 *   ┃ Commit: ...       ┃                   ┃
 *   ┃ Parents: ...      ┃                   ┃
 *   ┃ Author: ...       ┃                   ┃
 *   ┃ Date: ...         ┃                   ┃
 *   ┃                   ┃                   ┃
 *   ┣━━━━━━━━━━━━━━━━━━━┫                   ┃
 *   ┃ StatusCountView   ┃ FileDiffView      ┃
 *   ┠───────────────────┨                   ┃
 *   ┃                   ┃                   ┃
 *   ┃                   ┃                   ┃
 *   ┃ FileStatusView    ┃                   ┃
 *   ┃                   ┃                   ┃
 *   ┃                   ┃                   ┃
 *   ┗━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @see hamburg.remme.tinygit.domain.service.CommitDetailsService
 * @see FileStatusView
 * @see StatusCountView
 */
class CommitDetailsView : SplitPaneBuilder() {

    private val logService = TinyGit.commitLogService
    private val detailsService = TinyGit.commitDetailsService

    init {
        addClass(DEFAULT_STYLE_CLASS)

        val files = FileStatusView(detailsService.commitStatus).vgrow(Priority.ALWAYS)

        +splitPane {
            addClass(CONTENT_STYLE_CLASS)

            +webView {
                isContextMenuEnabled = false
                detailsService.commitDetails.addListener { _, _, it -> engine.loadContent(it) }
            }
            +stackPane {
                +vbox {
                    +toolBar { +StatusCountView(files.items) }
                    +files
                }
                +stackPane {
                    addClass(OVERLAY_STYLE_CLASS)
                    visibleWhen(Bindings.isEmpty(files.items))
                    managedWhen(visibleProperty())
                    +label { +I18N["commitDetails.noChanges"] }
                }
            }
        }
        +FileDiffView(files.selectionModel.selectedItemProperty(), logService.activeCommit)
    }

}
