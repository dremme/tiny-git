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
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.scrollPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.DonutChart
import hamburg.remme.tinygit.gui.component.HistogramChart
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.shortDateFormat
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.Tooltip
import javafx.scene.layout.Priority
import java.time.DayOfWeek
import java.time.LocalDate
import javafx.scene.chart.XYChart.Data as XYData

class StatsView : Tab() {

    private val repoService = TinyGit.repositoryService
    private val statsService = TinyGit.statsService
    private val contributions: DonutChart
    private val files: DonutChart
    private val commits: HistogramChart
    private val lines: HistogramChart
    private val activityData = observableList<XYData<LocalDate, DayOfWeek>>() // TODO

    init {
        text = I18N["stats.tab"]
        graphic = Icons.chartPie()
        isClosable = false

        statsService.contributorsData.addListener(ListChangeListener { updateContributions(it.list) })
        statsService.filesData.addListener(ListChangeListener { updateFiles(it.list) })
        statsService.commitsData.addListener(ListChangeListener { updateCommits(it.list) })
        statsService.activityData.addListener(ListChangeListener { updateActivity(it.list) })
        statsService.linesData.addListener(ListChangeListener { updateLines(it.list) })

        contributions = DonutChart(I18N["stats.contributionChart"])
        contributions.prefHeight = 400.0
        val contributionIndicator = ProgressIndicator(contributions)
        statsService.contributorsListener = contributionIndicator

        files = DonutChart(I18N["stats.fileChart"])
        files.prefHeight = 400.0
        val filesIndicator = ProgressIndicator(files)
        statsService.filesListener = filesIndicator

        commits = HistogramChart(I18N["stats.dailyContributionChart"])
        commits.prefHeight = 300.0
        commits.lowerBound = statsService.firstDay
        commits.upperBound = statsService.lastDay
        val commitsIndicator = ProgressIndicator(commits).columnSpan(2)
        statsService.commitsListener = commitsIndicator

        lines = HistogramChart(I18N["stats.locChart"])
        lines.prefHeight = 250.0
        lines.lowerBound = statsService.firstDay
        lines.upperBound = statsService.lastDay
        val linesIndicator = ProgressIndicator(lines).columnSpan(2)
        statsService.linesListener = linesIndicator

        val activity = CalendarChart(activityData)
        activity.prefHeight = 250.0
        activity.title = I18N["stats.activityChart"]
        val activityIndicator = ProgressIndicator(activity).columnSpan(2)
        statsService.activityListener = activityIndicator

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
                    +listOf(contributionIndicator,
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

    private fun updateContributions(data: List<DonutChart.Data>) {
        contributions.setData(data, { I18N["stats.descContrib", it] })
        data.forEach { it -> Tooltip.install(it.arc, Tooltip("${it.name} (${I18N["stats.commits", it.value]})")) }
    }

    private fun updateFiles(data: List<DonutChart.Data>) {
        files.setData(data, { I18N["stats.descFiles", it] })
        data.forEach { it -> Tooltip.install(it.arc, Tooltip("${it.name} (${I18N["stats.files", it.value]})")) }
    }

    private fun updateCommits(series: List<HistogramChart.Series>) {
        commits.setSeries(series)
        series.forEach { s ->
            s.data.forEach { Tooltip.install(it.rect, Tooltip("${s.name} (${I18N["stats.commits", it.yValue]})")) }
        }
    }

    private fun updateLines(series: List<HistogramChart.Series>) {
        lines.setSeries(series)
        series[0].data.forEach { it -> Tooltip.install(it.rect, Tooltip("${I18N["stats.added"]} (${I18N["stats.lines", it.yValue]})")) }
        series[1].data.forEach { it -> Tooltip.install(it.rect, Tooltip("${I18N["stats.removed"]} (${I18N["stats.lines", it.yValue]})")) }
    }

    private fun updateActivity(data: List<XYData<LocalDate, DayOfWeek>>) {
        activityData.setAll(data)
        activityData.forEach {
            Tooltip.install(it.node, Tooltip("${it.xValue.format(shortDateFormat)} (${I18N["stats.commits", it.extraValue as Int]})"))
        }
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
