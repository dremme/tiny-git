package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.Icons
import hamburg.remme.tinygit.gui.builder.ProgressPaneBuilder
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.progressPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.toObservableList
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Tab
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javafx.scene.chart.PieChart as FXPieChart


typealias PieData = FXPieChart.Data

class StatsView : Tab() {

    private val progressPane: ProgressPaneBuilder
    private val contributionData = observableList<PieData>()
    private val fileData = observableList<PieData>()
    private val commitData = observableList<XYChart.Series<String, Number>>()
    private var task: Task<*>? = null

    init {
        text = "Statistics (beta)"
        graphic = Icons.chartPie()
        isClosable = false

        val months = Month.values().map { it.getDisplayName(TextStyle.FULL, Locale.ROOT) }

        val contributions = PieChart(contributionData)
        contributions.title = "Contributions"

        val files = PieChart(fileData)
        files.title = "Files"

        val xAxis = CategoryAxis(months.toObservableList())
        val yAxis = NumberAxis()
        yAxis.isMinorTickVisible = false
        val commits = BarChart<String, Number>(xAxis, yAxis, commitData)
        commits.columnSpan(2)
        commits.title = "Commits by Month"
        commits.isLegendVisible = false
        commitData += XYChart.Series(months.map { XYChart.Data(it, 0 as Number) }.toObservableList())

        progressPane = progressPane {
            +vbox {
                +toolBar {
                    addSpacer()
                    +comboBox<String> {
                        isDisable = true
                        value = "This Year"
                    }
                }
                +grid(2) {
                    vgrow(Priority.ALWAYS)
                    addColumn(50.0, 50.0)
                    addRow(50.0, 50.0)
                    +listOf(contributions, files, commits)
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

            override fun call() {
                val currentYear = LocalDateTime.of(LocalDate.now().year, 1, 1, 0, 0)

                contribution = Git.log(repository, currentYear)
                        .groupingBy { it.authorMail }.eachCount()
                        .map { (author, value) -> author to value }
                        .sortedBy { (_, value) -> value }
                        .takeLast(5)

                files = Git.tree(repository)
                        .groupingBy { it.substringAfterLast('.', it.substringAfterLast('/')) }.eachCount()
                        .map { (ext, value) -> ext to value }
                        .sortedBy { (_, value) -> value }
                        .takeLast(5)

                commits = Git.log(repository, currentYear)
                        .groupingBy { it.date.month }.eachCount()
                        .map { (month, value) -> month to value }
                        .sortedBy { (month, _) -> month.value }
            }

            override fun succeeded() {
                contributionData.setAll(contribution.map { (author, value) -> PieData(author, value.toDouble()) })
                fileData.setAll(files.map { (ext, value) -> PieData(ext, value.toDouble()) })
                commitData[0].data.forEach { it.yValue = 0 }
                commits.forEach { (month, value) -> commitData[0].data[month.value - 1].yValue = value }
            }

            override fun failed() = exception.printStackTrace()
        }.also { progressPane.execute(it) }
    }

    private class PieChart(data: ObservableList<PieData>) : FXPieChart(data) {

        override fun layoutChartChildren(top: Double, left: Double, contentWidth: Double, contentHeight: Double) {
            data.forEach { data ->
                val label = lookupAll(".chart-pie-label").firstOrNull { it is Text && it.text.contains(data.name) } as? Text
                label?.text = data.pieValue.toInt().toString()
            }
            super.layoutChartChildren(top, left, contentWidth, contentHeight)
        }

    }

}
