package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.Refreshable
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StashService
import hamburg.remme.tinygit.domain.service.WorkingCopyService
import javafx.beans.binding.Bindings
import javafx.beans.binding.IntegerExpression
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task
import java.util.concurrent.Executors

object State {

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * THREAD POOLS                                                                                                  *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val cachedThreadPool = Executors.newCachedThreadPool({
        Executors.defaultThreadFactory().newThread(it).apply { isDaemon = true }
    })
    private val runningProcesses = SimpleIntegerProperty(0)
    private val processText = ReadOnlyStringWrapper()

    fun processTextProperty() = processText.readOnlyProperty!!

    fun execute(task: Task<*>) = cachedThreadPool.execute(task)

    fun startProcess(message: String, task: Task<*>) {
        task.setOnSucceeded { runningProcesses.dec() }
        task.setOnCancelled { runningProcesses.dec() }
        task.setOnFailed { runningProcesses.dec() }
        processText.set(message)
        runningProcesses.inc()
        cachedThreadPool.execute(task)
    }

    private fun IntegerProperty.inc() = set(get() + 1)
    private fun IntegerProperty.dec() = set(get() - 1)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * VISIBILITY                                                                                                    *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val showGlobalInfo = Bindings.isEmpty(RepositoryService.existingRepositories)!!
    val showGlobalOverlay = runningProcesses.greater0()!!
    val showToolBar = MergeService.isMerging.not().and(RebaseService.isRebasing.not())!!
    val showMergeBar = MergeService.isMerging
    val showRebaseBar = RebaseService.isRebasing
    val isModal = SimpleBooleanProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val isIdle = RepositoryService.activeRepository.isNotNull.and(runningProcesses.equals0())!!
    private val isReady = isIdle.and(MergeService.isMerging.not()).and(RebaseService.isRebasing.not())!!
    val canRemove = isReady
    val canSettings = isReady
    val canCommit = isReady.and(Bindings.isNotEmpty(WorkingCopyService.staged))!!
    val canPush = isReady.and(DivergenceService.ahead.unequals0())!!
    val canForcePush = canPush
    val canPull = isReady.and(DivergenceService.behind.greater0())!!
    val canFetch = isReady
    val canGc = canFetch
    val canBranch = isReady
    val canMerge = isReady.and(BranchService.branchesSize.greater1())!!
    val canMergeContinue = isIdle.and(MergeService.isMerging)!!
    val canMergeAbort = isIdle.and(MergeService.isMerging)!!
    val canRebase = isReady.and(BranchService.branchesSize.greater1())!!
    val canRebaseContinue = isIdle.and(RebaseService.isRebasing)!!
    val canRebaseAbort = isIdle.and(RebaseService.isRebasing)!!
    val canStash = isReady.and(Bindings.isNotEmpty(WorkingCopyService.staged).or(Bindings.isNotEmpty(WorkingCopyService.pending)))!!
    val canApplyStash = isReady.and(StashService.stashSize.greater0())!!
    val canReset = isReady.and(DivergenceService.behind.greater0())!!
    val canSquash = isReady.and(DivergenceService.aheadDefault.greater1())!!

    val canStageAll = isIdle.and(Bindings.isNotEmpty(WorkingCopyService.pending))!!
    val canUpdateAll = isIdle.and(Bindings.isNotEmpty(WorkingCopyService.modifiedPending))!!
    val canStageSelected = isIdle.and(Bindings.size(WorkingCopyService.selectedPending).greater0())!!
    val canUnstageAll = isIdle.and(Bindings.isNotEmpty(WorkingCopyService.staged))!!
    val canUnstageSelected = isIdle.and(Bindings.size(WorkingCopyService.selectedStaged).greater0())!!

    private fun IntegerExpression.equals0() = isEqualTo(0)
    private fun IntegerExpression.unequals0() = isNotEqualTo(0)
    private fun IntegerExpression.greater0() = greaterThan(0)
    private fun IntegerExpression.greater1() = greaterThan(1)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * REFRESH                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val refreshListeners = mutableListOf<(Repository) -> Unit>()

    fun addRefreshListener(block: (Repository) -> Unit) {
        refreshListeners += block
    }

    fun fireRefresh() {
        RepositoryService.activeRepository.get()?.let { repository -> refreshListeners.forEach { it.invoke(repository) } }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * API                                                                                                           *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    fun addListeners(refreshable: Refreshable) {
        RepositoryService.activeRepository.addListener { _, _, it ->
            it?.let { refreshable.onRepositoryChanged(it) } ?: refreshable.onRepositoryDeselected()
        }
        addRefreshListener { refreshable.onRefresh(it) }
    }

}
