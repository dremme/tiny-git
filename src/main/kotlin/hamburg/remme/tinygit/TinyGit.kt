package hamburg.remme.tinygit

import com.sun.javafx.PlatformUtil
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitDetailsService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.CommitService
import hamburg.remme.tinygit.domain.service.DiffService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.Refreshable
import hamburg.remme.tinygit.domain.service.RemoteService
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StashService
import hamburg.remme.tinygit.domain.service.WorkingCopyService
import hamburg.remme.tinygit.git.gitGetCredentialHelper
import hamburg.remme.tinygit.git.gitIsInstalled
import hamburg.remme.tinygit.git.gitSetWincred
import hamburg.remme.tinygit.git.gitVersion
import hamburg.remme.tinygit.gui.GitView
import hamburg.remme.tinygit.gui.builder.fatalAlert
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.concurrent.Task
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

// TODO: the great clean-up tbd
// TODO: clean-up nested collection methods
// TODO: cache stuff in nested collection methods, like find {}
// TODO: clean-up nested let {} for map {}
// TODO: instead of also {} and apply {}, use onEach {} at the end of a statement chain
// TODO: boolean properties (and parameters) should start with 'is'
// TODO: use type inference with ... : get() = ...
fun main(args: Array<String>) {
    Locale.setDefault(Locale.ROOT)

    Font.loadFont("font/Roboto-Regular.ttf".asResource(), 13.0)
    Font.loadFont("font/Roboto-Bold.ttf".asResource(), 13.0)
    Font.loadFont("font/LiberationMono-Regular.ttf".asResource(), 12.0)
    Font.loadFont("font/fa-brands-400.ttf".asResource(), 14.0)
    Font.loadFont("font/fa-solid-900.ttf".asResource(), 14.0)

    Application.launch(TinyGit::class.java, *args)
}

class TinyGit : Application() {

    companion object {

        private val cpuCount = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
        private val daemonFactory = ThreadFactory { Executors.defaultThreadFactory().newThread(it).apply { isDaemon = true } }
        private val pool = Executors.newFixedThreadPool(cpuCount, daemonFactory)
        private val scheduler = Executors.newScheduledThreadPool(1, daemonFactory)
        private val listeners = mutableListOf<(Repository) -> Unit>()
        val settings: Settings = Settings()
        val repositoryService: RepositoryService = RepositoryService()
        val remoteService = RemoteService().addListeners()
        val branchService = BranchService().addListeners()
        val workingCopyService = WorkingCopyService().addListeners()
        val divergenceService = DivergenceService().addListeners()
        val mergeService = MergeService(workingCopyService).addListeners()
        val rebaseService = RebaseService().addListeners()
        val stashService = StashService().addListeners()
        val commitLogService = CommitLogService(repositoryService).addListeners()
        val commitDetailsService = CommitDetailsService(commitLogService).addListeners()
        val commitService = CommitService(workingCopyService).addListeners()
        val diffService = DiffService().addListeners()
        val state = State(
                repositoryService,
                branchService,
                workingCopyService,
                divergenceService,
                mergeService,
                rebaseService,
                stashService)
        private lateinit var application: Application
        private lateinit var stage: Stage

        fun <T : Refreshable> T.addListeners(): T {
            repositoryService.activeRepository.addListener { _, _, it -> it?.let { onRepositoryChanged(it) } ?: onRepositoryDeselected() }
            addListener { onRefresh(it) }
            return this
        }

        fun addListener(block: (Repository) -> Unit) {
            listeners += block
        }

        fun fireEvent() {
            repositoryService.activeRepository.get()?.let { repository -> listeners.forEach { it.invoke(repository) } }
        }

        fun execute(task: Task<*>) = pool.execute(task)

        fun execute(message: String, task: Task<*>) {
            task.setOnSucceeded { state.runningProcesses.dec() }
            task.setOnCancelled { state.runningProcesses.dec() }
            task.setOnFailed { state.runningProcesses.dec() }
            state.processText.set(message)
            state.runningProcesses.inc()
            execute(task)
        }

        fun showDocument(uri: String) = application.hostServices.showDocument(uri)

    }

    private val title = "TinyGit ${javaClass.`package`.implementationVersion ?: ""}"

    override fun start(stage: Stage) {
        TinyGit.application = this
        TinyGit.stage = stage

        // We terminate here because technical requirements for TinyGit aren't met
        if (!gitIsInstalled()) {
            fatalAlert("Git Error", "Git is not installed or not in PATH.")
            showDocument("https://git-scm.com/downloads")
            System.exit(-1)
            return
        }
        if (gitVersion().major < 2) {
            fatalAlert("Git Error", "The installed Git client is out of date.\nPlease install the newest version.")
            showDocument("https://git-scm.com/downloads")
            System.exit(-1)
            return
        }
        if (PlatformUtil.isWindows() && gitGetCredentialHelper().isBlank()) gitSetWincred()

        settings.setWindow { Settings.WindowSettings(stage.x, stage.y, stage.width, stage.height, stage.isMaximized, stage.isFullScreen) }
        settings.load {
            stage.x = it.window.x
            stage.y = it.window.y
            stage.width = it.window.width.takeIf { it > 1.0 } ?: 1280.0
            stage.height = it.window.height.takeIf { it > 1.0 } ?: 800.0
            stage.isMaximized = it.window.maximized
            stage.isFullScreen = it.window.fullscreen
        }

        stage.focusedProperty().addListener { _, _, it ->
            if (it) {
                if (state.isModal.get()) state.isModal.set(false)
                else fireEvent()
            }
        }
        stage.scene = Scene(GitView())
        stage.scene.stylesheets += "default.css".asResource()
        stage.icons += Image("icon.png".asResource())
        stage.titleProperty().bind(Bindings.createStringBinding(Callable { updateTitle() },
                repositoryService.activeRepository,
                mergeService.isMerging,
                rebaseService.isRebasing,
                rebaseService.rebaseNext,
                rebaseService.rebaseLast))
        stage.show()

        scheduler.scheduleAtFixedRate({ if (!stage.isFocused && !state.isModal.get()) Platform.runLater { fireEvent() } }, 0, 10, TimeUnit.SECONDS)
    }

    override fun stop() = settings.save()

    private fun updateTitle(): String {
        val repository = repositoryService.activeRepository.get()?.let {
            val rebase = if (rebaseService.isRebasing.get()) "REBASING ${rebaseService.rebaseNext.get()}/${rebaseService.rebaseLast.get()} " else ""
            val merge = if (mergeService.isMerging.get()) "MERGING " else ""
            "${it.shortPath} [$it] $merge$rebase\u2012 "
        }
        return "${repository ?: ""}$title"
    }

}
