package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.flatten
import hamburg.remme.tinygit.git.gitBlame
import hamburg.remme.tinygit.git.gitDiffNumstat
import hamburg.remme.tinygit.git.gitLog
import hamburg.remme.tinygit.git.gitLsTree
import hamburg.remme.tinygit.mapParallel
import hamburg.remme.tinygit.observableList
import javafx.concurrent.Task
import java.time.LocalDate
import java.time.Year

class StatsService {

    val contributionData = observableList<Pair<String, Int>>()
    val filesData = observableList<Pair<String, Int>>()
    val commitsData = observableList<Pair<LocalDate, Int>>()
    val activityData = observableList<Pair<LocalDate, Int>>()
    val linesData = observableList<Pair<LocalDate, NumStat>>()
    lateinit var contributionMonitor: TaskMonitor
    lateinit var filesMonitor: TaskMonitor
    lateinit var commitsMonitor: TaskMonitor
    lateinit var activityMonitor: TaskMonitor
    lateinit var linesMonitor: TaskMonitor
    private val log = mutableListOf<Commit>()
    private val lastDay = LocalDate.now()
    private val firstDay = Year.of(lastDay.year - 1).atMonth(lastDay.month).atDay(1)
    private val taskPool = mutableMapOf<String, Task<*>>()

    fun updateActivity() {
        taskPool["activity"] = object : Task<List<Pair<LocalDate, Int>>>() {
            override fun call() = log
                    .groupingBy { it.date.toLocalDate() }
                    .eachCount()
                    .map { (date, value) -> date to value }

            override fun succeeded() {
                activityData.setAll(value)
                activityMonitor.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.execute(it) }
    }

    fun updateContributions(repository: Repository) {
        taskPool["contributions"] = object : Task<List<Pair<String, Int>>>() {
            override fun call() = gitDiffNumstat(repository, log.last(), log[0])
                    .filter { it.added + it.removed > 0 }
                    .mapParallel { if (!isCancelled) gitBlame(repository, it.path, firstDay) else null }
                    .filterNotNull()
                    .flatten { i1, i2 -> i1 + i2 }
                    .filter { it.key != "not.committed.yet" }
                    .map { (author, value) -> author to value }
                    .sortedBy { (_, value) -> value }
                    .takeLast(8)

            override fun succeeded() {
                contributionData.setAll(value)
                contributionMonitor.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.execute(it) }
    }

    fun updateCommits() {
        taskPool["commits"] = object : Task<List<Pair<LocalDate, Int>>>() {
            override fun call() = log
                    .map { it.date.toLocalDate() }
                    .map { it.minusDays(it.dayOfWeek.value - 1L) }
                    .groupingBy { it }
                    .eachCount()
                    .map { (date, value) -> date to value }
                    .sortedBy { (date, _) -> date }

            override fun succeeded() {
                commitsData.setAll(value)
                commitsMonitor.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.execute(it) }
    }

    fun updateFiles(repository: Repository) {
        taskPool["files"] = object : Task<List<Pair<String, Int>>>() {
            override fun call() = gitLsTree(repository)
                    .groupingBy { it.substringAfterLast('.', it.substringAfterLast('/')) }
                    .eachCount()
                    .map { (ext, value) -> ext to value }
                    .sortedBy { (_, value) -> value }
                    .takeLast(8)

            override fun succeeded() {
                filesData.setAll(value)
                filesMonitor.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.execute(it) }
    }

    fun updateLines(repository: Repository) {
        taskPool["lines"] = object : Task<List<Pair<LocalDate, NumStat>>>() {
            override fun call() = (0..12).map { firstDay.plusMonths(it.toLong()) }
                    .map { date -> log.filter { it.date.month == date.month && it.date.year == date.year } }
                    .map {
                        val min = it.minBy { it.date }
                        val max = it.maxBy { it.date }
                        min to max
                    }
                    .mapParallel {
                        if (!isCancelled && it.first != null && it.second != null) gitDiffNumstat(repository, it.first as Commit, it.second as Commit)
                        else emptyList()
                    }
                    .mapIndexed { i, it -> NumStat(it.sumBy { it.added }, it.sumBy { it.removed }, firstDay.plusMonths(i.toLong()).month.name) }
                    .mapIndexed { i, it -> firstDay.plusMonths(i.toLong()) to it }

            override fun succeeded() {
                linesData.setAll(value)
                linesMonitor.done()
            }

            override fun failed() = exception.printStackTrace()
        }.also { TinyGit.execute(it) }
    }

    // TODO: really?
    fun update(repository: Repository) {
        cancel()
        log.clear()
        taskPool["main"] = object : Task<List<Commit>>() {
            override fun call() = gitLog(repository, firstDay, lastDay)

            override fun succeeded() {
                log.addAll(value)
                updateActivity()
                updateCommits()
                updateContributions(repository)
                updateFiles(repository)
                updateLines(repository)
            }
        }.also {
            contributionMonitor.started()
            filesMonitor.started()
            commitsMonitor.started()
            activityMonitor.started()
            linesMonitor.started()
            TinyGit.execute(it)
        }
    }

    fun cancel() {
        taskPool.forEach { _, it -> it.cancel() }
    }

}
