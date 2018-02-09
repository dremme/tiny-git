package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.component.skin.GraphViewSkin
import hamburg.remme.tinygit.gui.component.skin.GraphViewSkinBase
import hamburg.remme.tinygit.shortDateTimeFormat
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.value.ObservableStringValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.text.Text
import java.util.concurrent.Callable

class GraphView(entries: ObservableList<Commit>, val head: ObservableStringValue, val branches: ObservableList<Branch>)
    : ListView<Commit>(entries) {

    private lateinit var skin: GraphViewSkinBase
    private lateinit var graphPadding: ObjectBinding<Insets>

    init {
        addClass("graph-view")
        setCellFactory { CommitLogListCell() }
        head.addListener { _ -> refresh() }
        branches.addListener(ListChangeListener { refresh() })
    }

    override fun createDefaultSkin(): GraphViewSkinBase {
        skin = GraphViewSkin(this)
        graphPadding = Bindings.createObjectBinding(Callable { Insets(0.0, 0.0, 0.0, skin.getGraphWidth()) }, skin.graphWidthProperty())
        return skin
    }

    private inner class CommitLogListCell : ListCell<Commit>() {

        private val MAX_LENGTH = 60
        private val commitId = Text().addClass("commitId")
        private val date = Text().addClass("date")
        private val badges = HBox().addClass("branches")
        private val message = Text().addClass("message")
        private val author = Text().addClass("author")

        init {
            graphic = vbox {
                addClass("graph-view-cell")
                paddingProperty().bind(graphPadding)
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
                badges.children.setAll(branches.filter { it.id == c.id }.map { it.name }.toBadges())
                message.text = c.shortMessage
                author.text = " ― ${c.authorName}"
            }
        }

        private fun List<String>.toBadges(): List<Node> {
            return map {
                label {
                    addClass("branch-badge")
                    if (it == head.get()) addClass("current")
                    text = it.abbrev()
                    graphic = Icons.codeFork()
                }
            }
        }

        private fun String.abbrev() = if (length > MAX_LENGTH) "${substring(0, MAX_LENGTH)}..." else this

    }

}
