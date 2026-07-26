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
                val last = if (yearEnd.isAfter(today)) today else yearEnd
                first to last
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

    private fun finishAllListeners() {
        Platform.runLater {
            contributorsListener.done()
            filesListener.done()
            commitsListener.done()
            activityListener.done()
            linesListener.done()
        }
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

                override fun failed() = exception.printStackTrace()
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

                override fun failed() = exception.printStackTrace()
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

                override fun failed() = exception.printStackTrace()
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

                override fun failed() = exception.printStackTrace()
            }.execute()
    }

    fun updateLines(repository: Repository) {
        taskPool +=
            object : Task<Unit>() {
                private val added = mutableListOf<HistogramChart.Data>()
                private val removed = mutableListOf<HistogramChart.Data>()

                override fun call() =
                    (0..lastDay.daysBetween(firstDay))
                        .map { firstDay.plusDays(it) }
                        .map { it.atStartOfWeek() }
                        .distinct()
                        .map { date -> date to log.filter { it.date.toLocalDate().atStartOfWeek() == date } }
                        .map { (date, log) ->
                            val min = log.minByOrNull { it.date }
                            val max = log.maxByOrNull { it.date }
                            Triple(date, min, max)
                        }.mapParallel { (date, first, last) ->
                            date to
                                if (!isCancelled && first != null && last != null) {
                                    gitDiffNumstat(repository, first, last)
                                } else {
                                    emptyList()
                                }
                        }.map { (date, stats) -> Triple(date, stats.sumOf { it.added }, stats.sumOf { it.removed }) }
                        .filter { (_, added, removed) -> added + removed > 0 }
                        .forEach { (date, added, removed) ->
                            this.added += HistogramChart.Data(date, added.toLong(), 7)
                            this.removed += HistogramChart.Data(date, removed.toLong(), 7)
                        }

                override fun succeeded() {
                    linesData.setAll(HistogramChart.Series("", added), HistogramChart.Series("", removed))
                    linesListener.done()
                }

                override fun failed() = exception.printStackTrace()
            }.execute()
    }

    fun update(repository: Repository) {
        cancel()
        log.clear()
        numStat.clear()

        val (rangeStart, rangeEnd) = period.resolveRange()
        firstDay = rangeStart
        lastDay = rangeEnd

        taskPool +=
            object : Task<Unit>() {
                private lateinit var log: List<Commit>
                private lateinit var numStat: List<NumStat>
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
                    log = gitLog(repository, firstDay, lastDay)
                    numStat =
                        if (log.isNotEmpty()) {
                            gitDiffNumstat(repository, log.last(), log[0])
                        } else {
                            emptyList()
                        }
                }

                override fun succeeded() {
                    availableYears.setAll(years)
                    rangeListener?.invoke()
                    this@StatsService.log += log
                    this@StatsService.numStat += numStat
                    if (log.isEmpty()) {
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

                override fun failed() {
                    finishAllListeners()
                }
            }.execute()
    }

    fun cancel() {
        taskPool.forEach { it.cancel() }
    }
}
