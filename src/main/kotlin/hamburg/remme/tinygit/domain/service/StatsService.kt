package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDiffNumstat
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.takeHighest
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import javafx.scene.chart.PieChart.Data as PieData
import javafx.scene.chart.XYChart.Data as XYData
import javafx.scene.chart.XYChart.Series as XYSeries

class StatsService {

    val numberOfAuthors = SimpleIntegerProperty()
    val numberOfFiles = SimpleIntegerProperty()
    val numberOfLines = SimpleIntegerProperty()
    val contributorsData = observableList<PieData>()
    val filesData = observableList<PieData>()
    val commitsData = observableList<XYSeries<LocalDate, Number>>()
    val activityData = observableList<XYData<LocalDate, DayOfWeek>>()
    val linesAddedData = observableList<XYData<LocalDate, Number>>()
    val linesRemovedData = observableList<XYData<LocalDate, Number>>()
    lateinit var contributorsListener: TaskListener
    lateinit var filesListener: TaskListener
    lateinit var commitsListener: TaskListener
    lateinit var activityListener: TaskListener
    lateinit var linesListener: TaskListener
    private val log = mutableListOf<Commit>()
    private val numStat = mutableListOf<NumStat>()
    private val lastDay = LocalDate.now()
    private val firstDay = Year.of(lastDay.year - 1).atMonth(lastDay.month).atDay(1)
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
        taskPool += object : Task<List<PieData>>() {
            override fun call() = log
                    .groupingBy { it.authorMail.toLowerCase() }
                    .eachCount()
                    .takeHighest(8)
                    .map { (author, value) -> PieData(author, value.toDouble()) }

            override fun succeeded() {
                contributorsData.setAll(value)
                contributorsListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateCommits() {
        taskPool += object : Task<List<XYSeries<LocalDate, Number>>>() {
            override fun call() = log
                    .groupBy { it.authorMail.toLowerCase() }
                    .mapValues { (_, value) ->
                        value.map { it.date.toLocalDate() }
                                .map { it.minusDays(it.dayOfWeek.value - 1L) }
                                .groupingBy { it }
                                .eachCount()
                                .toList()
                                .sortedBy { it.first }
                    }
                    .toList()
                    .sortedBy { (_, data) -> data.sumBy { it.second } }
                    .takeLast(8)
                    .map { (author, data) ->
                        XYSeries(author, observableList(data.map { (date, value) -> XYData<LocalDate, Number>(date, value) }))
                    }

            override fun succeeded() {
                commitsData.setAll(value)
                commitsListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateFiles() {
        taskPool += object : Task<List<PieData>>() {
            override fun call() = numStat
                    .map { it.path }
                    .groupingBy { it.substringAfterLast('.', it.substringAfterLast('/')) }
                    .eachCount()
                    .takeHighest(8)
                    .map { (ext, value) -> PieData(ext, value.toDouble()) }

            override fun succeeded() {
                filesData.setAll(value)
                filesListener.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.executeSlowly(it) }
    }

    fun updateLines(repository: Repository) {
        taskPool += object : Task<Unit>() {
            private val added = mutableListOf<XYData<LocalDate, Number>>()
            private val removed = mutableListOf<XYData<LocalDate, Number>>()

            override fun call() {
                (0..12).map { firstDay.plusMonths(it.toLong()) }
                        .map { date -> log.filter { it.date.month == date.month && it.date.year == date.year } }
                        .map {
                            val min = it.minBy { it.date }
                            val max = it.maxBy { it.date }
                            min to max
                        }
                        .map { (first, last) ->
                            if (!isCancelled && first != null && last != null) gitDiffNumstat(repository, first, last) else emptyList()
                        }
                        .map { it.sumBy { it.added } to it.sumBy { it.removed } }
                        .mapIndexed { i, it -> firstDay.plusMonths(i.toLong()) to it }
                        .filter { (_, stat) -> stat.first + stat.second > 0 }
                        .forEach { (date, value) ->
                            added += XYData<LocalDate, Number>(date, value.first)
                            removed += XYData<LocalDate, Number>(date, value.second)
                        }
            }

            override fun succeeded() {
                linesAddedData.setAll(added)
                linesRemovedData.setAll(removed)
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
