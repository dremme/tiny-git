package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.service.TaskListener
import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.scrollPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.DayOfYearAxis
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.shortDateFormat
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.PieChart
import javafx.scene.chart.StackedAreaChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Tab
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import javafx.util.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.Callable
import javafx.scene.chart.PieChart.Data as PieData
import javafx.scene.chart.XYChart.Data as XYData
import javafx.scene.chart.XYChart.Series as XYSeries

class StatsView : Tab() {

    private val repoService = TinyGit.repositoryService
    private val statsService = TinyGit.statsService
    private val contributorsData = observableList<PieData>()
    private val filesData = observableList<PieData>()
    private val commitsData = observableList<XYSeries<LocalDate, Number>>()
    private val activityData = observableList<XYData<LocalDate, DayOfWeek>>()
    private val linesAddedData = observableList<XYData<LocalDate, Number>>()
    private val linesRemovedData = observableList<XYData<LocalDate, Number>>()
    private val numberOfAuthors = SimpleIntegerProperty()
    private val numberOfFiles = SimpleIntegerProperty()
    private val numberOfLines = SimpleIntegerProperty()
    private val linesTimeline = Timeline()
    private val authorsTimeline = Timeline()
    private val filesTimeline = Timeline()

    init {
        text = I18N["stats.tab"]
        graphic = Icons.chartPie()
        isClosable = false

        statsService.contributorsData.addListener(ListChangeListener { updateContributions(it.list) })
        statsService.filesData.addListener(ListChangeListener { updateFiles(it.list) })
        statsService.commitsData.addListener(ListChangeListener { updateCommits(it.list) })
        statsService.activityData.addListener(ListChangeListener { updateActivity(it.list) })
        statsService.linesAddedData.addListener(ListChangeListener { linesAddedData.setAll(it.list) })
        statsService.linesRemovedData.addListener(ListChangeListener { linesRemovedData.setAll(it.list) })
        statsService.numberOfAuthors.addListener { _, _, it -> authorsTimeline.play(numberOfAuthors, it.toInt()) }
        statsService.numberOfFiles.addListener { _, _, it -> filesTimeline.play(numberOfFiles, it.toInt()) }
        statsService.numberOfLines.addListener { _, _, it -> linesTimeline.play(numberOfLines, it.toInt()) }

        val contributions = PieChart(contributorsData)
        contributions.title = I18N["stats.contributors"]
        contributions.labelsVisible = false
        val contributionIndicator = ProgressIndicator(contributions)
        statsService.contributorsListener = contributionIndicator

        val files = PieChart(filesData)
        files.title = "Files"
        files.labelsVisible = false
        val filesIndicator = ProgressIndicator(files)
        statsService.filesListener = filesIndicator

        val commits = StackedAreaChart<LocalDate, Number>(DayOfYearAxis(), NumberAxis().apply { isMinorTickVisible = false }, commitsData)
        commits.addClass("contributors")
        commits.prefHeight = 300.0
        commits.title = I18N["stats.weeklyContributors"]
        commits.createSymbols = false
        commits.isHorizontalZeroLineVisible = false
        commits.isVerticalZeroLineVisible = false
        val commitsIndicator = ProgressIndicator(commits).columnSpan(2)
        statsService.commitsListener = commitsIndicator

        val activity = CalendarChart(activityData)
        activity.prefHeight = 250.0
        activity.title = I18N["stats.activity"]
        val activityIndicator = ProgressIndicator(activity).columnSpan(2)
        statsService.activityListener = activityIndicator

        val lines = StackedAreaChart<LocalDate, Number>(
                DayOfYearAxis(), NumberAxis().apply { isMinorTickVisible = false },
                observableList(XYChart.Series(I18N["stats.removed"], linesRemovedData), XYChart.Series(I18N["stats.added"], linesAddedData)))
        lines.addClass("lines-of-code")
        lines.prefHeight = 250.0
        lines.title = I18N["stats.loc"]
        lines.createSymbols = false
        lines.isHorizontalZeroLineVisible = false
        lines.isVerticalZeroLineVisible = false
        val linesIndicator = ProgressIndicator(lines).columnSpan(2)
        statsService.linesListener = linesIndicator

        content = vbox {
            +toolBar {
                addSpacer()
                +button {
                    graphic = Icons.refresh()
                    tooltip = Tooltip(I18N["stats.refresh"])
                    setOnAction { statsService.update(repoService.activeRepository.get()!!) }
                }
                +comboBox<CalendarChart.Period> {
                    isDisable = true // TODO: implement changing periods
                    items.addAll(CalendarChart.Period.values())
                    valueProperty().addListener { _, _, it -> activity.updateYear(it) } // TODO: implement changing periods
                    value = CalendarChart.Period.LAST_YEAR
                }
            }
            +scrollPane {
                addClass("stats-view")
                vgrow(Priority.ALWAYS)
                isFitToWidth = true
                +grid(2) {
                    columns(50.0, 50.0)
                    +listOf(
                            hbox {
                                columnSpan(2)
                                addClass("synopsis")
                                +vbox {
                                    +label {
                                        addClass("header")
                                        textProperty().bind(Bindings.createStringBinding(Callable { I18N["stats.authors", numberOfAuthors.get()] }, numberOfAuthors))
                                        +Icons.user()
                                    }
                                    +label { +"...${I18N["stats.headAuthors"]}" }
                                }
                                +vbox {
                                    +label {
                                        addClass("header")
                                        textProperty().bind(Bindings.createStringBinding(Callable { I18N["stats.files", numberOfFiles.get()] }, numberOfFiles))
                                        +Icons.file()
                                    }
                                    +label { +"...${I18N["stats.headFiles"]}" }
                                }
                                +vbox {
                                    +label {
                                        addClass("header")
                                        textProperty().bind(Bindings.createStringBinding(Callable { I18N["stats.lines", numberOfLines.get()] }, numberOfLines))
                                        +Icons.code()
                                    }
                                    +label { +"...${I18N["stats.headLines"]}" }
                                }
                            },
                            contributionIndicator,
                            filesIndicator,
                            activityIndicator,
                            commitsIndicator,
                            linesIndicator)
                }
            }
        }

        repoService.activeRepository.addListener { _, _, it ->
            if (isSelected) it?.let { statsService.update(it) }
            else statsService.cancel()
        }
        selectedProperty().addListener { _, _, it ->
            if (it) statsService.update(repoService.activeRepository.get()!!)
            else statsService.cancel()
        }
    }

    private fun updateActivity(data: List<XYData<LocalDate, DayOfWeek>>) {
        activityData.setAll(data)
        activityData.tooltip { x, _, z -> "${x.format(shortDateFormat)} (${I18N["stats.commits", z as Int]})" }
    }

    private fun updateContributions(data: List<PieData>) {
        contributorsData.pieUpsert(data)
        contributorsData.tooltip { name, value -> "$name (${I18N["stats.commits", value]})" }
    }

    private fun updateCommits(data: List<XYSeries<LocalDate, Number>>) {
        commitsData.xyUpsert(data)
        commitsData.tooltip { it }
    }

    private fun updateFiles(data: List<PieData>) {
        filesData.pieUpsert(data)
        filesData.tooltip { name, value -> "$name (${I18N["stats.files", value]})" }
    }

    private fun ObservableList<PieData>.pieUpsert(data: List<PieData>) {
        if (isEmpty()) {
            setAll(data)
        } else {
            if (size > data.size) remove(data.size, size)
            forEachIndexed { i, it ->
                it.name = data[i].name
                it.pieValue = data[i].pieValue
            }
            if (size < data.size) addAll(data.subList(size, data.size))
        }
    }

    private fun <X, Y> ObservableList<XYSeries<X, Y>>.xyUpsert(data: List<XYSeries<X, Y>>) {
        if (isEmpty()) {
            setAll(data)
        } else {
            if (size > data.size) remove(data.size, size)
            forEachIndexed { i, it ->
                it.name = data[i].name
                it.data.setAll(data[i].data)
            }
            if (size < data.size) addAll(data.subList(size, data.size))
        }
    }

    private inline fun List<PieData>.tooltip(block: (String, Long) -> String) {
        forEach {
            Tooltip.uninstall(it.node, null)
            Tooltip.install(it.node, Tooltip(block.invoke(it.name, it.pieValue.toLong())))
        }
    }

    private inline fun <X, Y> List<XYSeries<X, Y>>.tooltip(block: (String) -> String) {
        forEach {
            Tooltip.uninstall(it.node, null)
            Tooltip.install(it.node, Tooltip(block.invoke(it.name)))
        }
    }

    private inline fun <X, Y> List<XYData<X, Y>>.tooltip(block: (X, Y, Any?) -> String) {
        forEach {
            Tooltip.uninstall(it.node, null)
            Tooltip.install(it.node, Tooltip(block.invoke(it.xValue, it.yValue, it.extraValue)))
        }
    }

    private fun Timeline.play(property: IntegerProperty, value: Int) {
        stop()
        keyFrames.setAll(KeyFrame(Duration.millis(1000.0), KeyValue(property, value, Interpolator.EASE_OUT)))
        play()
    }

    private class ProgressIndicator(content: Node) : StackPaneBuilder(), TaskListener {

        private val visible = SimpleBooleanProperty()

        init {
            +content.visibleWhen(visible.not())
            +progressIndicator(16.0).visibleWhen(visible)
        }

        override fun started() = visible.set(true)

        override fun done() = visible.set(false)

    }

}
