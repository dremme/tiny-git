package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.Service
import hamburg.remme.tinygit.TaskListener
import hamburg.remme.tinygit.atStartOfWeek
import hamburg.remme.tinygit.daysBetween
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.execute
import hamburg.remme.tinygit.git.gitDiffNumstat
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.git.gitLogYears
import hamburg.remme.tinygit.gui.component.CalendarChart
import hamburg.remme.tinygit.gui.component.DonutChart
import hamburg.remme.tinygit.gui.component.HistogramChart
import hamburg.remme.tinygit.mapParallel
import hamburg.remme.tinygit.mapValuesParallel
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.sortedBy
import javafx.application.Platform
import javafx.concurrent.Task
import java.time.LocalDate

sealed class StatsPeriod {
    data object LastTwelveMonths : StatsPeriod()

    data class StatsYear(
        val year: Int,
    ) : StatsPeriod()

    fun resolveRange(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> =
        when (this) {
            is LastTwelveMonths -> {
                val first =
                    java.time.Year
                        .of(today.year - 1)
                        .atMonth(today.month)
                        .atDay(1)
                first to today
            }
            is StatsYear -> {
                val first = LocalDate.of(year, 1, 1)
                val yearEnd = LocalDate.of(year, 12, 31)
                first to minOf(yearEnd, today)
            }
        }
}

@Service
class StatsService {
    val contributorsData = observableList<DonutChart.Data>()
    val filesData = observableList<DonutChart.Data>()
    val commitsData = observableList<HistogramChart.Series>()
    val linesData = observableList<HistogramChart.Series>()
    val activityData = observableList<CalendarChart.Data>()
    val availableYears = observableList<Int>()
    var period: StatsPeriod = StatsPeriod.LastTwelveMonths
    var lastDay: LocalDate = LocalDate.now()
        private set
    var firstDay: LocalDate = period.resolveRange().first
        private set

    lateinit var contributorsListener: TaskListener
    lateinit var filesListener: TaskListener
    lateinit var commitsListener: TaskListener
    lateinit var activityListener: TaskListener
    lateinit var linesListener: TaskListener

    var rangeListener: (() -> Unit)? = null
    private val log = mutableListOf<Commit>()
    private val numStat = mutableListOf<NumStat>()
    private val taskPool = mutableSetOf<Task<*>>()

    private fun finishAllListeners() =
        Platform.runLater {
            contributorsListener.done()
            filesListener.done()
            commitsListener.done()
            activityListener.done()
            linesListener.done()
        }

    fun updateActivity() {
        taskPool +=
            object : Task<List<CalendarChart.Data>>() {
                override fun call() =
                    log
                        .groupingBy { it.date.toLocalDate() }
                        .eachCount()
                        .map { (date, value) -> CalendarChart.Data(date, value) }

                override fun succeeded() {
                    activityData.setAll(value)
                    activityListener.done()
                }

                override fun failed() {
                    exception.printStackTrace()
                    activityListener.done()
                }
            }.execute()
    }

    fun updateContributors() {
        taskPool +=
            object : Task<List<DonutChart.Data>>() {
                override fun call() =
                    log
                        .groupingBy { it.authorMail.lowercase() }
                        .eachCount()
                        .sortedBy { it.second }
                        .map { (author, value) -> DonutChart.Data(author, value.toLong()) }

                override fun succeeded() {
                    contributorsData.setAll(value)
                    contributorsListener.done()
                }

                override fun failed() {
                    exception.printStackTrace()
                    contributorsListener.done()
                }
            }.execute()
    }

    fun updateCommits() {
        taskPool +=
            object : Task<List<HistogramChart.Series>>() {
                override fun call() =
                    log
                        .groupBy { it.authorMail.lowercase() }
                        .mapValuesParallel {
                            if (!isCancelled) {
                                it
                                    .map { it.date.toLocalDate() }
                                    .groupingBy { it }
                                    .eachCount()
                                    .toList()
                                    .sortedBy { it.first }
                            } else {
                                emptyList()
                            }
                        }.toList()
                        .sortedBy { (_, data) -> data.sumOf { it.second } }
                        .map { (author, data) ->
                            HistogramChart.Series(author, data.map { (date, value) -> HistogramChart.Data(date, value.toLong()) })
                        }

                override fun succeeded() {
                    commitsData.setAll(value)
                    commitsListener.done()
                }

                override fun failed() {
                    exception.printStackTrace()
                    commitsListener.done()
                }
            }.execute()
    }

    fun updateFiles() {
        taskPool +=
            object : Task<List<DonutChart.Data>>() {
                override fun call() =
                    numStat
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

                override fun failed() {
                    exception.printStackTrace()
                    filesListener.done()
                }
            }.execute()
    }

    fun updateLines(repository: Repository) {
        taskPool +=
            object : Task<Unit>() {
                private val added = mutableListOf<HistogramChart.Data>()
                private val removed = mutableListOf<HistogramChart.Data>()

                override fun call() {
                    (0..lastDay.daysBetween(firstDay))
                        .map { firstDay.plusDays(it).atStartOfWeek() }
                        .distinct()
                        .map { week -> week to log.filter { it.date.toLocalDate().atStartOfWeek() == week } }
                        .mapParallel { (week, weekLog) ->
                            val first = weekLog.minByOrNull { it.date }
                            val last = weekLog.maxByOrNull { it.date }
                            week to
                                if (!isCancelled && first != null && last != null) {
                                    gitDiffNumstat(repository, first, last)
                                } else {
                                    emptyList()
                                }
                        }.map { (week, stats) -> Triple(week, stats.sumOf { it.added }, stats.sumOf { it.removed }) }
                        .filter { (_, a, r) -> a + r > 0 }
                        .forEach { (week, a, r) ->
                            added += HistogramChart.Data(week, a.toLong(), 7)
                            removed += HistogramChart.Data(week, r.toLong(), 7)
                        }
                }

                override fun succeeded() {
                    linesData.setAll(HistogramChart.Series("", added), HistogramChart.Series("", removed))
                    linesListener.done()
                }

                override fun failed() {
                    exception.printStackTrace()
                    linesListener.done()
                }
            }.execute()
    }

    fun update(repository: Repository) {
        cancel()

        val (rangeStart, rangeEnd) = period.resolveRange()
        firstDay = rangeStart
        lastDay = rangeEnd

        taskPool +=
            object : Task<Unit>() {
                private lateinit var loadedLog: List<Commit>
                private lateinit var loadedNumStat: List<NumStat>
                private lateinit var years: List<Int>

                override fun call() {
                    Platform.runLater {
                        contributorsListener.started()
                        filesListener.started()
                        commitsListener.started()
                        activityListener.started()
                        linesListener.started()
                    }
                    years = gitLogYears(repository)
                    loadedLog = gitLog(repository, firstDay, lastDay)
                    loadedNumStat =
                        if (loadedLog.isNotEmpty()) {
                            gitDiffNumstat(repository, loadedLog.last(), loadedLog[0])
                        } else {
                            emptyList()
                        }
                }

                override fun succeeded() {
                    availableYears.setAll(years)
                    rangeListener?.invoke()
                    // Replace data only after load so cancelled background work still sees the old list.
                    log.clear()
                    log += loadedLog
                    numStat.clear()
                    numStat += loadedNumStat
                    if (loadedLog.isEmpty()) {
                        contributorsData.clear()
                        filesData.clear()
                        commitsData.clear()
                        activityData.clear()
                        linesData.clear()
                        finishAllListeners()
                        return
                    }
                    updateActivity()
                    updateCommits()
                    updateContributors()
                    updateFiles()
                    updateLines(repository)
                }

                override fun failed() = finishAllListeners()
            }.execute()
    }

    fun cancel() {
        taskPool.forEach { it.cancel() }
        taskPool.clear()
    }
}
