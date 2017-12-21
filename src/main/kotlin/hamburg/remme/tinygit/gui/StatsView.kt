package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.ProgressPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.progressPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.component.PieChart
import hamburg.remme.tinygit.observableList
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.control.Tab
import javafx.scene.layout.Priority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale
import javafx.scene.chart.PieChart.Data as PieData
import javafx.scene.chart.XYChart.Data as XYData

class StatsView : Tab() {

    private val progressPane: ProgressPaneBuilder
    private val contributionData = observableList<PieData>()
    private val fileData = observableList<PieData>()
    private val commitData = observableList<PieData>()
    private val calendarData = observableList<XYData<LocalDate, DayOfWeek>>()
    private var task: Task<*>? = null

    init {
        text = "Statistics (beta)"
        graphic = Icons.chartPie()
        isClosable = false

        val contributions = PieChart(contributionData, "commits")
        contributions.title = "Contributions"

        val files = PieChart(fileData, "files")
        files.title = "Files"

        val commits = PieChart(commitData, "commits")
        commits.title = "Commits by Month"

        val calendar = CalendarChart(calendarData)
        calendar.columnSpan(3)
        calendar.title = "Activity"

        progressPane = progressPane {
            +vbox {
                +toolBar {
                    addSpacer()
                    +comboBox<String> {
                        isDisable = true
                        value = "This Year"
                    }
                }
                +grid(3) {
                    addClass("stats-view")
                    vgrow(Priority.ALWAYS)
                    addColumn(33.333, 33.333, 33.333)
                    addRow(65.0, 35.0)
                    +listOf(contributions, commits, files, calendar)
                }
            }
        }
        content = progressPane

        State.addRepositoryListener { it?.let { update(it) } }
        State.addRefreshListener(this) { update(it) }
    }

    private fun update(repository: LocalRepository) {
        task?.cancel()
        task = object : Task<Unit>() {
            lateinit var contribution: List<Pair<String, Int>>
            lateinit var files: List<Pair<String, Int>>
            lateinit var commits: List<Pair<Month, Int>>
            lateinit var calendar: List<Pair<LocalDate, Int>>

            override fun call() {
                val log = Git.log(repository, Year.now().atDay(1).atStartOfDay()).toList()

                contribution = log.groupingBy { it.authorMail }
                        .eachCount()
                        .map { (author, value) -> author to value }
                        .sortedBy { (_, value) -> value }
                        .takeLast(8)

                files = Git.tree(repository)
                        .groupingBy { it.substringAfterLast('.', it.substringAfterLast('/')) }
                        .eachCount()
                        .map { (ext, value) -> ext to value }
                        .sortedBy { (_, value) -> value }
                        .takeLast(8)

                commits = log.groupingBy { it.date.month }
                        .eachCount()
                        .map { (month, value) -> month to value }
                        .sortedBy { (month, _) -> month.value }

                calendar = log.groupingBy { it.date.toLocalDate() }
                        .eachCount()
                        .map { (date, value) -> date to value }
            }

            override fun succeeded() {
                contributionData.setPieData(contribution.map { (author, value) -> PieData(author, value.toDouble()) })
                fileData.setPieData(files.map { (ext, value) -> PieData(ext, value.toDouble()) })
                commitData.setPieData(commits.map { (month, value) -> PieData(month.getDisplayName(TextStyle.FULL, Locale.ROOT), value.toDouble()) })
                // TODO: this is still refreshing every time
                calendarData.setAll(calendar.map { (date, value) -> XYData(date, date.dayOfWeek, value) })
            }

            override fun failed() = exception.printStackTrace()

            private fun ObservableList<PieData>.setPieData(data: List<PieData>) {
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
        }.also { progressPane.execute(it) }
    }

}