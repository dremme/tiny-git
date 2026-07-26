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
import javafx.collections.ObservableList
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
 * Commit list with optional graph overlay via [GraphListViewSkin].
 *
 * [commits] should be [CommitLogService.commits] or a filtered view of it.
 */
class GraphListView(
    commits: ObservableList<Commit>,
) : ListView<Commit>(commits) {
    private val branchService = TinyGit.get<BranchService>()
    private val tagService = TinyGit.get<TagService>()

    private val graphVisibleProperty = SimpleBooleanProperty(true)
    var isGraphVisible: Boolean
        get() = graphVisibleProperty.get()
        set(value) = graphVisibleProperty.set(value)

    /** Left cell padding reserved for the graph; only publishes when the value changes. */
    private val graphWidthProperty = SimpleObjectProperty(Insets.EMPTY)!!
    var graphWidth: Double
        get() = graphWidthProperty.get().left
        set(value) {
            if (graphWidthProperty.get().left != value) {
                graphWidthProperty.set(Insets(0.0, 0.0, 0.0, value))
            }
        }

    init {
        addClass(DEFAULT_STYLE_CLASS)
        cellFactory = Callback { GraphListCell() }
        branchService.head.addListener { _ -> refresh() }
        branchService.branches.addListener(ListChangeListener { refresh() })
        tagService.tags.addListener(ListChangeListener { refresh() })
    }

    override fun createDefaultSkin() = GraphListViewSkin(this)

    private inner class GraphListCell : ListCell<Commit>() {
        private val commitId = label { addClass(COMMIT_STYLE_CLASS) }
        private val date =
            label {
                addClass(DATE_STYLE_CLASS)
                graphic = Icons.calendar()
            }
        private val badges = hbox { addClass(BRANCHES_STYLE_CLASS) }
        private val message = label { addClass(MESSAGE_STYLE_CLASS) }
        private val author =
            label {
                addClass(AUTHOR_STYLE_CLASS)
                graphic = Icons.user()
            }
        private var badgesKey: String? = null

        init {
            graphic =
                vbox {
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

        override fun updateItem(
            item: Commit?,
            empty: Boolean,
        ) {
            super.updateItem(item, empty)
            graphic.isVisible = !empty
            if (empty || item == null) {
                badgesKey = null
                badges.children.clear()
                return
            }
            commitId.text = item.shortId
            date.text = item.date.format(shortDateTimeFormat)
            message.text = item.shortMessage
            author.text = item.authorName
            updateBadges(item)
        }

        private fun updateBadges(commit: Commit) {
            val tags = tagService.tags.filter { it.id == commit.id }
            val branches = branchService.branches.filter { it.id == commit.id }
            val key =
                buildString {
                    append(commit.id)
                    tags.forEach { append('|').append(it.name) }
                    branches.forEach {
                        append('|').append(it.name)
                        append(':').append(branchService.isDetached(it))
                        append(':').append(branchService.isHead(it))
                    }
                }
            if (key == badgesKey) return
            badgesKey = key
            badges.children.setAll(tags.toTagBadges() + branches.toBranchBadges())
        }

        private fun List<Branch>.toBranchBadges(): List<Node> =
            map { branch ->
                label {
                    addClass(BRANCH_BADGE_STYLE_CLASS)
                    when {
                        branchService.isDetached(branch) -> addClass(DETACHED_STYLE_CLASS)
                        branchService.isHead(branch) -> addClass(CURRENT_STYLE_CLASS)
                    }
                    text = branch.name.abbrev()
                    graphic = if (branchService.isDetached(branch)) Icons.locationArrow() else Icons.codeBranch()
                }
            }

        private fun List<Tag>.toTagBadges(): List<Node> =
            map { tag ->
                label {
                    addClass(TAG_BADGE_STYLE_CLASS)
                    text = tag.name
                    graphic = Icons.tag()
                }
            }

        private fun String.abbrev() = if (length > MAX_LENGTH) "${take(MAX_LENGTH)}..." else this
    }
}
