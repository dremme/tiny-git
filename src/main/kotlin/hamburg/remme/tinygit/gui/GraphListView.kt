package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Tag
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.TagService
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.shortDateTimeFormat
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

private const val DEFAULT_STYLE_CLASS = "graph-list-view"
private const val COMMIT_STYLE_CLASS = "commitId"
private const val DATE_STYLE_CLASS = "date"
private const val BRANCHES_STYLE_CLASS = "branches"
private const val MESSAGE_STYLE_CLASS = "message"
private const val AUTHOR_STYLE_CLASS = "author"
private const val BRANCH_BADGE_STYLE_CLASS = "branch-badge"
private const val TAG_BADGE_STYLE_CLASS = "tag-badge"
private const val DETACHED_STYLE_CLASS = "detached"
private const val CURRENT_STYLE_CLASS = "current"
private const val MAX_LENGTH = 60

/**
 * This view has some heavy interaction with [GraphListViewSkin] but is still loosely coupled, as it would
 * work with the default list skin, just without Git log graph.
 *
 * The actual log graph is calculated asynchronously by [CommitLogService] when the log changes.
 *
 * @todo: skin should not extend ListViewSkin but wrap a [ListView]?!
 * @todo: this class should be a control?!
 */
class GraphListView : ListView<Commit>(TinyGit.get<CommitLogService>().commits) {

    private val branchService = TinyGit.get<BranchService>()
    private val tagService = TinyGit.get<TagService>()

    init {
        addClass(DEFAULT_STYLE_CLASS)
        cellFactory = Callback { GraphListCell() }
        branchService.head.addListener { _ -> refresh() }
        branchService.branches.addListener(ListChangeListener { refresh() })
        tagService.tags.addListener(ListChangeListener { refresh() })
    }

    override fun createDefaultSkin() = GraphListViewSkin(this)

    /* --------------------------------------------------
     * GRAPH VISIBLE
     * -------------------------------------------------- */
    private val graphVisibleProperty = SimpleBooleanProperty(true)
    var isGraphVisible: Boolean
        get() = graphVisibleProperty.get()
        set(value) = graphVisibleProperty.set(value)

    /* --------------------------------------------------
     * GRAPH PADDING
     * -------------------------------------------------- */
    private val graphWidthProperty = SimpleObjectProperty<Insets>()
    var graphWidth: Double
        get() = graphWidthProperty.get().left
        set(value) = graphWidthProperty.set(Insets(0.0, 0.0, 0.0, value))

    /**
     * This rather complex list cell is displaying brief information about the commit.
     * It will show its ID, commit time, message and author.
     *
     * It will also display any branch pointing to the commit.
     *
     * The [ListCell] will have a left padding bound to [GraphListView.graphVisibleProperty] to leave space for the
     * graph that is drawn by the [GraphListViewSkin].
     *
     * @todo branches all have the same color which is not synchronized with the log graph
     */
    private inner class GraphListCell : ListCell<Commit>() {

        private val commitId = label { addClass(COMMIT_STYLE_CLASS) }
        private val date = label {
            addClass(DATE_STYLE_CLASS)
            graphic = Icons.calendar()
        }
        private val badges = hbox { addClass(BRANCHES_STYLE_CLASS) }
        private val message = label { addClass(MESSAGE_STYLE_CLASS) }
        private val author = label {
            addClass(AUTHOR_STYLE_CLASS)
            graphic = Icons.user()
        }

        init {
            graphic = vbox {
                paddingProperty().bind(graphWidthProperty)
                +hbox {
                    alignment = Pos.CENTER_LEFT
                    +commitId
                    +date
                    +badges
                }
                +hbox {
                    alignment = Pos.CENTER_LEFT
                    +message
                    +author
                }
            }
        }

        override fun updateItem(item: Commit?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic.isVisible = !empty
            item?.let { c ->
                commitId.text = c.shortId
                date.text = c.date.format(shortDateTimeFormat)
                badges.children.setAll(tagService.tags.filter { it.id == c.id }.toTagBadges())
                badges.children.addAll(branchService.branches.filter { it.id == c.id }.toBranchBadges())
                message.text = c.shortMessage
                author.text = c.authorName
            }
        }

        private fun List<Branch>.toBranchBadges(): List<Node> {
            return map {
                label {
                    addClass(BRANCH_BADGE_STYLE_CLASS)
                    if (branchService.isDetached(it)) addClass(DETACHED_STYLE_CLASS)
                    else if (branchService.isHead(it)) addClass(CURRENT_STYLE_CLASS)
                    text = it.name.abbrev()
                    graphic = if (branchService.isDetached(it)) Icons.locationArrow() else Icons.codeFork()
                }
            }
        }

        private fun List<Tag>.toTagBadges(): List<Node> {
            return map {
                label {
                    addClass(TAG_BADGE_STYLE_CLASS)
                    text = it.name
                    graphic = Icons.tag()
                }
            }
        }

        private fun String.abbrev() = if (length > MAX_LENGTH) "${substring(0, MAX_LENGTH)}..." else this

    }

}
