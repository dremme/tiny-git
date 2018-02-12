package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.service.TaskMonitor
import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
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
import hamburg.remme.tinygit.gui.component.PieChart
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.chart.AreaChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.ComboBox
import javafx.scene.control.Tab
import javafx.scene.layout.Priority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javafx.scene.chart.PieChart.Data as PieData
import javafx.scene.chart.XYChart.Data as XYData
import javafx.scene.chart.XYChart.Series as XYSeries

class StatsView : Tab() {

    private val repoService = TinyGit.repositoryService
    private val statsService = TinyGit.statsService
    private val period: ComboBox<CalendarChart.Period>
    private val contributionData = observableList<PieData>()
    private val filesData = observableList<PieData>()
    private val commitsData = observableList<PieData>()
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

        val contributions = PieChart(contributionData, "commits")
        contributions.title = "Contributions"
        val contributionIndicator = ProgressIndicator(contributions)
        statsService.contributionMonitor = contributionIndicator

        val files = PieChart(filesData, "files")
        files.title = "Files"
        val filesIndicator = ProgressIndicator(files)
        statsService.filesMonitor = filesIndicator

        val commits = PieChart(commitsData, "commits")
        commits.title = "Commits by Month"
        val commitsIndicator = ProgressIndicator(commits)
        statsService.commitsMonitor = commitsIndicator

        val activity = CalendarChart(activityData)
        activity.prefHeight = 250.0
        activity.title = "Activity"
        val activityIndicator = ProgressIndicator(activity).columnSpan(3)
        statsService.activityMonitor = activityIndicator

        val lines = AreaChart<LocalDate, Number>(DayOfYearAxis(), NumberAxis(),
                observableList(XYChart.Series("Added", addedLinesData), XYChart.Series("Removed", removedLinesData)))
        lines.prefHeight = 250.0
        lines.title = "Lines of Code by Month"
        lines.isHorizontalZeroLineVisible = false
        lines.isVerticalZeroLineVisible = false
        val linesIndicator = ProgressIndicator(lines).columnSpan(3)
        statsService.linesMonitor = linesIndicator

        period = comboBox {
            isDisable = true // TODO: implement changing periods
            items.addAll(CalendarChart.Period.values())
            valueProperty().addListener { _, _, it -> activity.updateYear(it) } // TODO: implement changing periods
            value = CalendarChart.Period.LAST_YEAR
        }
        content = vbox {
            +toolBar {
                addSpacer()
                +period
            }
            +scrollPane {
                addClass("stats-view")
                vgrow(Priority.ALWAYS)
                isFitToWidth = true
                +grid(3) {
                    columns(33.333, 33.333, 33.333)
                    +listOf(activityIndicator,
                            linesIndicator,
                            contributionIndicator,
                            commitsIndicator,
                            filesIndicator)
                }
            }
        }

        repoService.activeRepository.addListener { _, _, it -> it?.let { statsService.update(it) } }
        selectedProperty().addListener { _, _, it -> if (it) statsService.update(repoService.activeRepository.get()!!) }
    }

    private fun updateActivity(data: List<Pair<LocalDate, Int>>) {
        activityData.setAll(data.map { (date, value) -> XYData(date, date.dayOfWeek, value) })
    }

    private fun updateContributions(data: List<Pair<String, Int>>) {
        contributionData.upsert(data.map { (author, value) -> PieData(author, value.toDouble()) })
    }

    private fun updateCommits(data: List<Pair<Month, Int>>) {
        commitsData.upsert(data.map { (month, value) -> PieData(month.getDisplayName(TextStyle.FULL, Locale.ROOT), value.toDouble()) })
    }

    private fun updateFiles(data: List<Pair<String, Int>>) {
        filesData.upsert(data.map { (ext, value) -> PieData(ext, value.toDouble()) })
    }

    private fun updateLines(data: List<Pair<LocalDate, NumStat>>) {
        addedLinesData.setAll(data.map { (date, value) -> XYData<LocalDate, Number>(date, value.added) })
        removedLinesData.setAll(data.map { (date, value) -> XYData<LocalDate, Number>(date, value.removed) })
    }

    private fun ObservableList<PieData>.upsert(data: List<PieData>) {
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
