package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.service.TaskMonitor
import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.scrollPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.DayOfYearAxis
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.monthOfYearFormat
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.shortDateFormat
import hamburg.remme.tinygit.weekOfMonthFormat
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.chart.AreaChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.PieChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Tab
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import java.time.DayOfWeek
import java.time.LocalDate
import javafx.scene.chart.PieChart.Data as PieData
import javafx.scene.chart.XYChart.Data as XYData
import javafx.scene.chart.XYChart.Series as XYSeries

class StatsView : Tab() {

    private val repoService = TinyGit.repositoryService
    private val statsService = TinyGit.statsService
    private val contributionData = observableList<PieData>()
    private val filesData = observableList<PieData>()
    private val commitsData = observableList<XYData<LocalDate, Number>>()
    private val activityData = observableList<XYData<LocalDate, DayOfWeek>>()
    private val addedLinesData = observableList<XYData<LocalDate, Number>>()
    private val removedLinesData = observableList<XYData<LocalDate, Number>>()

    init {
        text = "Statistics (beta)"
        graphic = Icons.chartPie()
        isClosable = false

        statsService.contributionData.addListener(ListChangeListener { updateContributions(it.list) })
        statsService.filesData.addListener(ListChangeListener { updateFiles(it.list) })
        statsService.commitsData.addListener(ListChangeListener { updateCommits(it.list) })
        statsService.activityData.addListener(ListChangeListener { updateActivity(it.list) })
        statsService.linesData.addListener(ListChangeListener { updateLines(it.list) })

        val contributions = PieChart(contributionData)
        contributions.title = "Contributions"
        contributions.labelsVisible = false
        val contributionIndicator = ProgressIndicator(contributions)
        statsService.contributionMonitor = contributionIndicator

        val files = PieChart(filesData)
        files.title = "Files"
        files.labelsVisible = false
        val filesIndicator = ProgressIndicator(files)
        statsService.filesMonitor = filesIndicator

        val commits = AreaChart<LocalDate, Number>(
                DayOfYearAxis(), NumberAxis().apply { isMinorTickVisible = false },
                observableList(XYChart.Series(commitsData)))
        commits.prefHeight = 250.0
        commits.title = "Commits by Weeks"
        commits.isHorizontalZeroLineVisible = false
        commits.isVerticalZeroLineVisible = false
        commits.isLegendVisible = false
        val commitsIndicator = ProgressIndicator(commits).columnSpan(2)
        statsService.commitsMonitor = commitsIndicator

        val activity = CalendarChart(activityData)
        activity.prefHeight = 250.0
        activity.title = "Daily Activity"
        val activityIndicator = ProgressIndicator(activity).columnSpan(2)
        statsService.activityMonitor = activityIndicator

        val lines = AreaChart<LocalDate, Number>(
                DayOfYearAxis(), NumberAxis().apply { isMinorTickVisible = false },
                observableList(XYChart.Series("Added", addedLinesData), XYChart.Series("Removed", removedLinesData)))
        lines.addClass("chart-loc")
        lines.prefHeight = 250.0
        lines.title = "Lines of Code by Month"
        lines.isHorizontalZeroLineVisible = false
        lines.isVerticalZeroLineVisible = false
        val linesIndicator = ProgressIndicator(lines).columnSpan(2)
        statsService.linesMonitor = linesIndicator

        content = vbox {
            +toolBar {
                +button {
                    graphic = Icons.refresh()
                    tooltip = Tooltip("Refresh Stats")
                    setOnAction { statsService.update(repoService.activeRepository.get()!!) }
                }
                addSpacer()
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
                    +listOf(activityIndicator,
                            commitsIndicator,
                            linesIndicator,
                            contributionIndicator,
                            filesIndicator)
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

    private fun updateActivity(data: List<Pair<LocalDate, Int>>) {
        activityData.setAll(data.map { (date, value) -> XYData(date, date.dayOfWeek, value) })
        activityData.xyTooltips {
            val value = it.extraValue as Int
            "${it.xValue.format(shortDateFormat)} ($value commit${value.plural()})"
        }
    }

    private fun updateContributions(data: List<Pair<String, Int>>) {
        contributionData.pieUpsert(data.map { (author, value) -> PieData(author, value.toDouble()) })
        contributionData.pieTooltips { "${it.name} (${it.pieValue.toInt()} line${it.pieValue.plural()})" }
    }

    private fun updateCommits(data: List<Pair<LocalDate, Int>>) {
        commitsData.xyUpsert(data.map { (date, value) -> XYData<LocalDate, Number>(date, value) })
        commitsData.xyTooltips { "${it.xValue.format(weekOfMonthFormat)} (${it.yValue} commit${it.yValue.plural()})" }
    }

    private fun updateFiles(data: List<Pair<String, Int>>) {
        filesData.pieUpsert(data.map { (ext, value) -> PieData(ext, value.toDouble()) })
        filesData.pieTooltips { "${it.name} (${it.pieValue.toInt()} file${it.pieValue.plural()})" }
    }

    private fun updateLines(data: List<Pair<LocalDate, NumStat>>) {
        addedLinesData.xyUpsert(data.map { (date, value) -> XYData<LocalDate, Number>(date, value.added) })
        removedLinesData.xyUpsert(data.map { (date, value) -> XYData<LocalDate, Number>(date, value.removed) })
        addedLinesData.xyTooltips { "${it.xValue.format(monthOfYearFormat)} (${it.yValue} line${it.yValue.plural()})" }
        removedLinesData.xyTooltips { "${it.xValue.format(monthOfYearFormat)} (${it.yValue} line${it.yValue.plural()})" }
    }

    private fun Number.plural() = if (toLong() > 1) "s" else ""

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

    private fun <X, Y> ObservableList<XYData<X, Y>>.xyUpsert(data: List<XYData<X, Y>>) {
        if (isEmpty()) {
            setAll(data)
        } else {
            if (size > data.size) remove(data.size, size)
            forEachIndexed { i, it ->
                it.xValue = data[i].xValue
                it.yValue = data[i].yValue
            }
            if (size < data.size) addAll(data.subList(size, data.size))
        }
    }

    private fun ObservableList<PieData>.pieTooltips(block: (PieData) -> String) {
        forEach {
            Tooltip.uninstall(it.node, null)
            Tooltip.install(it.node, Tooltip(block.invoke(it)))
        }
    }

    private fun <X, Y> ObservableList<XYData<X, Y>>.xyTooltips(block: (XYData<X, Y>) -> String) {
        forEach {
            Tooltip.uninstall(it.node, null)
            Tooltip.install(it.node, Tooltip(block.invoke(it)))
        }
    }

    private class ProgressIndicator(content: Node) : StackPaneBuilder(), TaskMonitor {

        private val visible = SimpleBooleanProperty()

        init {
            +content.visibleWhen(visible.not())
            +progressIndicator(16.0).visibleWhen(visible)
        }

        override fun started() = visible.set(true)

        override fun done() = visible.set(false)

    }

}
