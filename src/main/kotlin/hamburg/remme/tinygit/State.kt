package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.service.BranchService
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

class State(repositoryService: RepositoryService,
            branchService: BranchService,
            workingCopyService: WorkingCopyService,
            divergenceService: DivergenceService,
            mergeService: MergeService,
            rebaseService: RebaseService,
            stashService: StashService) {

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
    val isModal = SimpleBooleanProperty()

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                                               *
     * ACTIONS                                                                                                       *
     *                                                                                                               *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    private val isIdle = repositoryService.activeRepository.isNotNull.and(runningProcesses.equals0())!!
    private val isReady = isIdle.and(mergeService.isMerging.not()).and(rebaseService.isRebasing.not())!!
    val canRemove = isReady
    val canSettings = isReady
    val canPush = isReady.and(divergenceService.ahead.unequals0())!!
    val canForcePush = canPush
    val canPull = isReady.and(divergenceService.behind.greater0())!!
    val canFetch = isReady
    val canGc = canFetch
    val canBranch = isReady
    val canMerge = isReady.and(branchService.branchesSize.greater1())!!
    val canMergeContinue = isIdle.and(mergeService.isMerging)!!
    val canMergeAbort = isIdle.and(mergeService.isMerging)!!
    val canRebase = isReady.and(branchService.branchesSize.greater1())!!
    val canRebaseContinue = isIdle.and(rebaseService.isRebasing)!!
    val canRebaseAbort = isIdle.and(rebaseService.isRebasing)!!
    val canStash = isReady.and(Bindings.isNotEmpty(workingCopyService.staged).or(Bindings.isNotEmpty(workingCopyService.pending)))!!
    val canApplyStash = isReady.and(stashService.stashSize.greater0())!!
    val canReset = isReady.and(divergenceService.behind.greater0())!!
    val canSquash = isReady.and(divergenceService.aheadDefault.greater1())!!

    val canCommit = isReady.and(Bindings.isNotEmpty(workingCopyService.staged))!!
    val canStageAll = isIdle.and(Bindings.isNotEmpty(workingCopyService.pending))!!
    val canUpdateAll = isIdle.and(Bindings.isNotEmpty(workingCopyService.modifiedPending))!!
    val canStageSelected = isIdle.and(Bindings.size(workingCopyService.selectedPending).greater0())!!
    val canUnstageAll = isIdle.and(Bindings.isNotEmpty(workingCopyService.staged))!!
    val canUnstageSelected = isIdle.and(Bindings.size(workingCopyService.selectedStaged).greater0())!!

}
