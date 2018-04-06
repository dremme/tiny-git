package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TaskListener
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StatsService
import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.scrollPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.tooltip
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.DonutChart
import hamburg.remme.tinygit.gui.component.HistogramChart
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.monthOfYearFormat
import hamburg.remme.tinygit.shortDateFormat
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.layout.Priority
import javafx.scene.chart.XYChart.Data as XYData

/**
 * Showing various Git statistics using the [TinyGit.statsService].
 * The query is asynchronous displaying a loading indicator.
 *
 * Will contain functionality to change the range of time queried and resolution for the statistics.
 * Currently the last year from today is shown.
 *
 * The view is showing:
 *  * [DonutChart] with the contributors and their number of commits.
 *  * [DonutChart] with the number of files by their type.
 *  * [CalendarChart] showing the number of commits over the time range and day of the week.
 *  * [HistogramChart] displaying a stacked bar chart with all commits by author.
 *  * [HistogramChart] displaying a stacked bar chart with the number of added and removed lines.
 *
 * **Git stats are very much beta as are the diagrams.**
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 *   ┃ ToolBar                    ┃
 *   ┠────────────────────────────┨
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃          Diagrams          ┃
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
 * @todo implement time range selection and filtering
 *
 * @see DonutChart
 * @see HistogramChart
 * @see CalendarChart
 */
class StatsView : Tab() {

    private val repoService = TinyGit.get<RepositoryService>()
    private val statsService = TinyGit.get<StatsService>()
    private val contributions: DonutChart
    private val files: DonutChart
    private val commits: HistogramChart
    private val lines: HistogramChart
    private val activity: CalendarChart

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
        commits.updateBoundsAndTicks()
        val commitsIndicator = ProgressIndicator(commits).columnSpan(2)
        statsService.commitsListener = commitsIndicator

        lines = HistogramChart(I18N["stats.locChart"])
        lines.prefHeight = 250.0
        lines.updateBoundsAndTicks()
        val linesIndicator = ProgressIndicator(lines).columnSpan(2)
        statsService.linesListener = linesIndicator

        activity = CalendarChart(I18N["stats.activityChart"])
        activity.prefHeight = 250.0
        activity.updateBoundsAndTicks()
        val activityIndicator = ProgressIndicator(activity).columnSpan(2)
        statsService.activityListener = activityIndicator

        content = vbox {
            +toolBar {
                addSpacer()
                +button {
                    tooltip(I18N["stats.refresh"])
                    graphic = Icons.refresh()
                    setOnAction { statsService.update(repoService.activeRepository.get()!!) }
                }
                // TODO: implement
                +comboBox<Any> {
                    isDisable = true
//                    items.addAll(CalendarChart.Period.values())
//                    valueProperty().addListener { _, _, it -> activity.updateYear(it) }
//                    value = CalendarChart.Period.LAST_YEAR
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

    private fun HistogramChart.updateBoundsAndTicks() {
        lowerBound = statsService.firstDay
        upperBound = statsService.lastDay
        setTickMarks((0L..12L).map { statsService.firstDay.plusMonths(it) }
                .map { HistogramChart.TickMark(monthOfYearFormat.format(it), it) })
    }

    private fun CalendarChart.updateBoundsAndTicks() {
        lowerBound = statsService.firstDay
        upperBound = statsService.lastDay
        setTickMarks((0L..12L).map { statsService.firstDay.plusMonths(it) }
                .map { CalendarChart.TickMark(monthOfYearFormat.format(it), it) })
    }

    private fun updateContributions(data: List<DonutChart.Data>) {
        contributions.setData(data, { I18N["stats.descContrib", it] })
        data.forEach { it -> it.node?.tooltip("${it.name} (${I18N["stats.commits", it.value]})") }
    }

    private fun updateFiles(data: List<DonutChart.Data>) {
        files.setData(data, { I18N["stats.descFiles", it] })
        data.forEach { it -> it.node?.tooltip("${it.name} (${I18N["stats.files", it.value]})") }
    }

    private fun updateCommits(series: List<HistogramChart.Series>) {
        commits.setSeries(series)
        series.forEach { s -> s.data.forEach { it.node?.tooltip("${s.name} (${I18N["stats.commits", it.yValue]})") } }
    }

    private fun updateLines(series: List<HistogramChart.Series>) {
        lines.setSeries(series)
        series[0].data.forEach { it -> it.node?.tooltip("${I18N["stats.added"]} (${I18N["stats.lines", it.yValue]})") }
        series[1].data.forEach { it -> it.node?.tooltip("${I18N["stats.removed"]} (${I18N["stats.lines", it.yValue]})") }
    }

    private fun updateActivity(data: List<CalendarChart.Data>) {
        activity.setData(data)
        data.forEach { it.node?.tooltip("${it.xValue.format(shortDateFormat)} (${I18N["stats.commits", it.yValue]})") }
    }

    /**
     * Wrapping a [Node] to show a progress indicator if needed.
     */
    private class ProgressIndicator(content: Node) : StackPaneBuilder(), TaskListener {

        private val visible = SimpleBooleanProperty()

        init {
            +content.visibleWhen(visible.not())
            +progressIndicator(2.0).visibleWhen(visible)
        }

        override fun started() = visible.set(true)

        override fun done() = visible.set(false)

    }

}
