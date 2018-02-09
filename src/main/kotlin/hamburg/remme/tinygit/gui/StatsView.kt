package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.service.TaskExecutor
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.git.gitLsTree
import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.columnSpan
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.grid
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.component.PieChart
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.Tab
import javafx.scene.layout.Priority
import javafx.util.Callback
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale
import javafx.scene.chart.PieChart.Data as PieData
import javafx.scene.chart.XYChart.Data as XYData

class StatsView : Tab() {

    private val repoService = TinyGit.repositoryService
    private val period: ComboBox<Year>
    private lateinit var log: List<Commit>

    private val contributionData = observableList<PieData>()
    private val filesData = observableList<PieData>()
    private val commitsData = observableList<PieData>()
    private val activityData = observableList<XYData<LocalDate, DayOfWeek>>()

    private val contributionIndicator: ProgressIndicator
    private val filesIndicator: ProgressIndicator
    private val commitsIndicator: ProgressIndicator
    private val activityIndicator: ProgressIndicator

    private var contributionTask: Task<*>? = null
    private var filesTask: Task<*>? = null
    private var commitsTask: Task<*>? = null
    private var activityTask: Task<*>? = null

    init {
        text = "Statistics (beta)"
        graphic = Icons.chartPie()
        isClosable = false

        val contributions = PieChart(contributionData, "commits")
        contributions.title = "Contributions"
        contributionIndicator = ProgressIndicator(contributions)

        val files = PieChart(filesData, "files")
        files.title = "Files"
        filesIndicator = ProgressIndicator(files)

        val commits = PieChart(commitsData, "commits")
        commits.title = "Commits by Month"
        commitsIndicator = ProgressIndicator(commits)

        val currentYear = Year.now()
        val activity = CalendarChart(activityData)
        activity.title = "Activity"
        activity.updateYear(currentYear)
        activityIndicator = ProgressIndicator(activity).columnSpan(3)

        period = comboBox {
            items.addAll(currentYear,
                    currentYear.minusYears(1),
                    currentYear.minusYears(2),
                    currentYear.minusYears(3),
                    currentYear.minusYears(4))
            buttonCell = PeriodListCell()
            cellFactory = Callback { PeriodListCell() }
            value = currentYear
            valueProperty().addListener { _, _, it ->
                activity.updateYear(it)
                update(repoService.activeRepository.get()!!, it)
            }
        }
        content = vbox {
            +toolBar {
                addSpacer()
                +period
            }
            +grid(3) {
                addClass("stats-view")
                vgrow(Priority.ALWAYS)
                columns(33.333, 33.333, 33.333)
                rows(35.0, 65.0)
                +listOf(activityIndicator,
                        contributionIndicator,
                        commitsIndicator,
                        filesIndicator)
            }
        }

        repoService.activeRepository.addListener { _, _, it -> it?.let { update(it, period.value) } }
        selectedProperty().addListener { _ -> repoService.activeRepository.get()?.let { update(it, period.value) } }
//        TinyGit.addListener { update(it, period.value) } TODO
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

    private fun update(repository: Repository, year: Year) {
        if (isSelected) TinyGit.execute(object : Task<List<Commit>>() {
            override fun call() = gitLog(repository, year.atDay(1), year.atDay(year.length()))
            override fun succeeded() {
                log = value
                updateActivity()
                updateCommits()
                updateContributions()
                updateFiles(repository)
            }
        })
    }

    private fun updateActivity() {
        activityTask?.cancel()
        activityTask = object : Task<List<Pair<LocalDate, Int>>>() {
            override fun call() = log
                    .groupingBy { it.date.toLocalDate() }
                    .eachCount()
                    .map { (date, value) -> date to value }

            override fun succeeded() {
                activityData.setAll(value.map { (date, value) -> XYData(date, date.dayOfWeek, value) })
            }

            override fun failed() = exception.printStackTrace()
        }.also { activityIndicator.execute(it) }
    }

    private fun updateContributions() {
        contributionTask?.cancel()
        contributionTask = object : Task<List<Pair<String, Int>>>() {
            override fun call() = log
                    .groupingBy { it.authorMail }
                    .eachCount()
                    .map { (author, value) -> author to value }
                    .sortedBy { (_, value) -> value }
                    .takeLast(8)

            override fun succeeded() {
                contributionData.upsert(value.map { (author, value) -> PieData(author, value.toDouble()) })
            }

            override fun failed() = exception.printStackTrace()
        }.also { contributionIndicator.execute(it) }
    }

    private fun updateCommits() {
        commitsTask?.cancel()
        commitsTask = object : Task<List<Pair<Month, Int>>>() {
            override fun call() = log
                    .groupingBy { it.date.month }
                    .eachCount()
                    .map { (month, value) -> month to value }
                    .sortedBy { (month, _) -> month.value }

            override fun succeeded() {
                commitsData.upsert(value.map { (month, value) -> PieData(month.getDisplayName(TextStyle.FULL, Locale.ROOT), value.toDouble()) })
            }

            override fun failed() = exception.printStackTrace()
        }.also { commitsIndicator.execute(it) }
    }

    private fun updateFiles(repository: Repository) {
        filesTask?.cancel()
        filesTask = object : Task<List<Pair<String, Int>>>() {
            override fun call() = gitLsTree(repository)
                    .groupingBy { it.substringAfterLast('.', it.substringAfterLast('/')) }
                    .eachCount()
                    .map { (ext, value) -> ext to value }
                    .sortedBy { (_, value) -> value }
                    .takeLast(8)

            override fun succeeded() {
                filesData.upsert(value.map { (ext, value) -> PieData(ext, value.toDouble()) })
            }

            override fun failed() = exception.printStackTrace()
        }.also { filesIndicator.execute(it) }
    }

    private class PeriodListCell : ListCell<Year>() {

        override fun updateItem(item: Year?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.value?.toString()
        }

    }

    private class ProgressIndicator(content: Node) : StackPaneBuilder(), TaskExecutor {

        private val visible = SimpleBooleanProperty()

        init {
            +content.visibleWhen(visible.not())
            +progressIndicator(16.0).visibleWhen(visible)
        }

        override fun execute(task: Task<*>) {
            task.setOnSucceeded { visible.set(false) }
            task.setOnCancelled { visible.set(false) }
            task.setOnFailed { visible.set(false) }
            visible.set(true)
            TinyGit.execute(task)
        }

    }

}
