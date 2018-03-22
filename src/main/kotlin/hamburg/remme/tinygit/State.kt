package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StashService
import hamburg.remme.tinygit.domain.service.WorkingCopyService
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty

/**
 * Central application state used mostly for action permissions.
 *
 * @todo probably requires some streamlining and clean-up.
 */
class State(repositoryService: RepositoryService,
            branchService: BranchService,
            workingCopyService: WorkingCopyService,
            divergenceService: DivergenceService,
            mergeService: MergeService,
            rebaseService: RebaseService,
            stashService: StashService,
            commitLogService: CommitLogService) {

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * PROCESS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val runningProcesses = SimpleIntegerProperty(0)
    val processText = SimpleStringProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * VISIBILITY                                                                                                    *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    val showGlobalInfo = Bindings.isEmpty(repositoryService.existingRepositories)!!
    val showGlobalOverlay = runningProcesses.greater0()
    val showToolBar = mergeService.isMerging.not().and(rebaseService.isRebasing.not())!!
    val showMergeBar = mergeService.isMerging
    val showRebaseBar = rebaseService.isRebasing
    val isModal = SimpleBooleanProperty(true)

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val isIdle = repositoryService.activeRepository.isNotNull.and(runningProcesses.equals0())!!
    private val isClean = isIdle.and(mergeService.isMerging.not()).and(rebaseService.isRebasing.not())!!
    val canRemove = isClean
    val canSettings = isClean
    val canPush = isClean.and(divergenceService.ahead.unequals0())!!
    val canForcePush = canPush
    val canPull = isClean.and(divergenceService.behind.greater0())!!
    val canFetch = isClean
    val canGc = canFetch
    val canBranch = isClean
    val canMerge = isClean.and(branchService.branchesSize.greater1())!!
    val canMergeContinue = isIdle.and(mergeService.isMerging)!!
    val canMergeAbort = isIdle.and(mergeService.isMerging)!!
    val canRebase = isClean.and(branchService.branchesSize.greater1())!!
    val canRebaseContinue = isIdle.and(rebaseService.isRebasing)!!
    val canRebaseAbort = isIdle.and(rebaseService.isRebasing)!!
    val canStash = isClean.and(Bindings.isNotEmpty(workingCopyService.staged).or(Bindings.isNotEmpty(workingCopyService.pending)))!!
    val canApplyStash = isClean.and(stashService.stashSize.greater0())!!
    val canReset = isClean.and(divergenceService.behind.greater0())!!
    val canSquash = isClean.and(divergenceService.aheadDefault.greater1())!!

    val canCommit = isClean.and(Bindings.isNotEmpty(workingCopyService.staged))!!
    val canStageAll = isIdle.and(Bindings.isNotEmpty(workingCopyService.pending))!!
    val canUpdateAll = isIdle.and(Bindings.isNotEmpty(workingCopyService.modifiedPending))!!
    val canStageSelected = isIdle.and(Bindings.size(workingCopyService.selectedPending).greater0())!!
    val canDeleteSelected = isIdle.and(Bindings.size(workingCopyService.selectedPending.filtered { it.status != File.Status.REMOVED }).greater0())!!
    val canDiscardSelected = isIdle.and(Bindings.size(workingCopyService.selectedPending.filtered { it.status != File.Status.ADDED }).greater0())!!
    val canUnstageAll = isIdle.and(Bindings.isNotEmpty(workingCopyService.staged))!!
    val canUnstageSelected = isIdle.and(Bindings.size(workingCopyService.selectedStaged).greater0())!!

    val canCheckoutCommit = isClean.and(commitLogService.activeCommit.isNotNull)!!
    val canResetToCommit = isClean.and(commitLogService.activeCommit.isNotNull)!!
    val canTagCommit = isClean.and(commitLogService.activeCommit.isNotNull)!!

    val canCmd = isIdle

}
