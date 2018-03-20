package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.daysBetween
import hamburg.remme.tinygit.daysFromOrigin
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDiffNumstat
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.gui.component.DonutChart
import hamburg.remme.tinygit.gui.component.HistogramChart
import hamburg.remme.tinygit.mapParallel
import hamburg.remme.tinygit.mapValuesParallel
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.sortedBy
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import javafx.scene.chart.XYChart.Data as XYData

class StatsService {

    val numberOfAuthors = SimpleIntegerProperty()
    val numberOfFiles = SimpleIntegerProperty()
    val numberOfLines = SimpleIntegerProperty()
    val contributorsData = observableList<DonutChart.Data>()
    val filesData = observableList<DonutChart.Data>()
    val commitsData = observableList<HistogramChart.Series>()
    val linesData = observableList<HistogramChart.Series>()
    val activityData = observableList<XYData<LocalDate, DayOfWeek>>()
    val lastDay = LocalDate.now()!!
    val firstDay = Year.of(lastDay.year - 1).atMonth(lastDay.month).atDay(1)!!
    lateinit var contributorsListener: TaskListener
    lateinit var filesListener: TaskListener
    lateinit var commitsListener: TaskListener
    lateinit var activityListener: TaskListener
    lateinit var linesListener: TaskListener
    private val log = mutableListOf<Commit>()
    private val numStat = mutableListOf<NumStat>()
    private val taskPool = mutableSetOf<Task<*>>()

    fun updateNumberOfAuthors() {
        numberOfAuthors.set(log.distinctBy { it.authorMail.toLowerCase() }.size)
    }

    fun updateNumberOfFiles() {
        numberOfFiles.set(numStat.size)
    }

    fun updateNumberOfLines() {
        numberOfLines.set(numStat.sumBy { it.added + it.removed })
    }

    fun updateActivity() {
        taskPool += object : Task<List<XYData<LocalDate, DayOfWeek>>>() {
            override fun call() = log
                    .groupingBy { it.date.toLocalDate() }
                    .eachCount()
                    .map { (date, value) -> XYData(date, date.dayOfWeek, value) }

            override fun succeeded() {
                activityData.setAll(value)
                activityListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateContributors() {
        taskPool += object : Task<List<DonutChart.Data>>() {
            override fun call() = log
                    .groupingBy { it.authorMail.toLowerCase() }
                    .eachCount()
                    .sortedBy { it.second }
                    .map { (author, value) -> DonutChart.Data(author, value.toLong()) }

            override fun succeeded() {
                contributorsData.setAll(value)
                contributorsListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateCommits() {
        taskPool += object : Task<List<HistogramChart.Series>>() {
            override fun call() = log
                    .groupBy { it.authorMail.toLowerCase() }
                    .mapValuesParallel {
                        it.map { it.date.toLocalDate() }
                                .groupingBy { it }
                                .eachCount()
                                .toList()
                                .sortedBy { it.first }
                    }
                    .toList()
                    .sortedBy { (_, data) -> data.sumBy { it.second } }
                    .map { (author, data) ->
                        HistogramChart.Series(author, data.map { (date, value) -> HistogramChart.Data(date.daysFromOrigin, value.toLong()) })
                    }

            override fun succeeded() {
                commitsData.setAll(value)
                commitsListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateFiles() {
        taskPool += object : Task<List<DonutChart.Data>>() {
            override fun call() = numStat
                    .map { it.path }
                    .groupingBy { it.substringAfterLast('.', it.substringAfterLast('/')) }
                    .eachCount()
                    .sortedBy { it.second }
                    .filter { (_, value) -> value > 0 }
                    .map { (ext, value) -> DonutChart.Data(ext, value.toLong()) }

            override fun succeeded() {
                filesData.setAll(value)
                filesListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateLines(repository: Repository) {
        taskPool += object : Task<Unit>() {
            private val added = mutableListOf<HistogramChart.Data>()
            private val removed = mutableListOf<HistogramChart.Data>()

            override fun call() = (0..lastDay.daysBetween(firstDay))
                    .map { firstDay.plusDays(it) }
                    .map { date -> log.filter { it.date.toLocalDate() == date } }
                    .map {
                        val min = it.minBy { it.date }
                        val max = it.maxBy { it.date }
                        min to max
                    }
                    .mapParallel { (first, last) ->
                        if (!isCancelled && first != null && last != null) gitDiffNumstat(repository, first, last) else emptyList()
                    }
                    .map { it.sumBy { it.added } to it.sumBy { it.removed } }
                    .mapIndexed { i, it -> firstDay.plusDays(i.toLong()) to it }
                    .filter { (_, stat) -> stat.first + stat.second > 0 }
                    .forEach { (date, value) ->
                        added += HistogramChart.Data(date.daysFromOrigin, value.first.toLong())
                        removed += HistogramChart.Data(date.daysFromOrigin, value.second.toLong())
                    }

            override fun succeeded() {
                linesData.setAll(HistogramChart.Series("", added), HistogramChart.Series("", removed))
                linesListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    // TODO: really?
    fun update(repository: Repository) {
        cancel()
        log.clear()
        numStat.clear()
        numberOfAuthors.set(0)
        numberOfFiles.set(0)
        numberOfLines.set(0)
        taskPool += object : Task<Unit>() {
            private lateinit var log: List<Commit>
            private lateinit var numStat: List<NumStat>

            override fun call() {
                log = gitLog(repository, firstDay, lastDay)
                numStat = gitDiffNumstat(repository, log.last(), log[0])
            }

            override fun succeeded() {
                this@StatsService.log.addAll(log)
                this@StatsService.numStat.addAll(numStat)
                updateNumberOfAuthors()
                updateNumberOfFiles()
                updateNumberOfLines()
                updateActivity()
                updateCommits()
                updateContributors()
                updateFiles()
                updateLines(repository)
            }
        }.also {
            contributorsListener.started()
            filesListener.started()
            commitsListener.started()
            activityListener.started()
            linesListener.started()
            TinyGit.execute(it)
        }
    }

    fun cancel() {
        taskPool.forEach { it.cancel() }
    }

}
