package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitDetailsService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.CommitService
import hamburg.remme.tinygit.domain.service.CredentialService
import hamburg.remme.tinygit.domain.service.DiffService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.Refreshable
import hamburg.remme.tinygit.domain.service.RemoteService
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StashService
import hamburg.remme.tinygit.domain.service.StatsService
import hamburg.remme.tinygit.domain.service.TagService
import hamburg.remme.tinygit.domain.service.WorkingCopyService
import hamburg.remme.tinygit.git.gitGetCredentialHelper
import hamburg.remme.tinygit.git.gitIsInstalled
import hamburg.remme.tinygit.git.gitSetKeychain
import hamburg.remme.tinygit.git.gitSetWincred
import hamburg.remme.tinygit.git.gitVersion
import hamburg.remme.tinygit.gui.GitView
import hamburg.remme.tinygit.gui.builder.fatalAlert
import hamburg.remme.tinygit.gui.dialog.CredentialsDialog
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
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    Locale.setDefault(Locale.ROOT)

    Font.loadFont("font/Roboto-Regular.ttf".asResource(), 13.0)
    Font.loadFont("font/Roboto-Bold.ttf".asResource(), 13.0)
    Font.loadFont("font/Roboto-Light.ttf".asResource(), 13.0)
    Font.loadFont("font/LiberationMono-Regular.ttf".asResource(), 12.0)
    Font.loadFont("font/fa-brands-400.ttf".asResource(), 14.0)
    Font.loadFont("font/fa-solid-900.ttf".asResource(), 14.0)

    Application.launch(TinyGit::class.java, *args)
}

class TinyGit : Application() {

    companion object {

        private val listeners = mutableListOf<(Repository) -> Unit>()
        val settings = Settings()
        val statsService = StatsService()
        val credentialService = CredentialService()
        val repositoryService: RepositoryService = RepositoryService(credentialService)
        val remoteService = RemoteService(repositoryService, credentialService).addListeners()
        val branchService = BranchService(repositoryService, credentialService).addListeners()
        val tagService = TagService().addListeners()
        val workingCopyService = WorkingCopyService().addListeners()
        val divergenceService = DivergenceService().addListeners()
        val mergeService = MergeService().addListeners()
        val rebaseService = RebaseService().addListeners()
        val stashService = StashService().addListeners()
        val commitLogService = CommitLogService(repositoryService, credentialService).addListeners()
        val commitDetailsService = CommitDetailsService(commitLogService).addListeners()
        val commitService = CommitService().addListeners()
        val diffService = DiffService().addListeners()
        val state = State(repositoryService, branchService, workingCopyService, divergenceService, mergeService, rebaseService, stashService, commitLogService)
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
            repositoryService.activeRepository.get()?.let { repository -> listeners.forEach { it(repository) } }
        }

        fun execute(task: Task<*>) = cachedPool.execute(task)

        fun executeSlowly(task: Task<*>) = singlePool.execute(task)

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

    init {
        TinyGit.application = this
    }

    override fun start(stage: Stage) {
        TinyGit.stage = stage

        // We terminate here because technical requirements for TinyGit aren't met
        if (isMac || isLinux) gitIsInstalled() // UNIX workaround
        if (!gitIsInstalled()) {
            fatalAlert(I18N["error.gitError"], I18N["error.gitNotInstalled"])
            showDocument("https://git-scm.com/downloads")
            System.exit(-1)
            return
        }
        if (gitVersion().major < 2) {
            fatalAlert(I18N["error.gitError"], I18N["error.gitOutOfDate"])
            showDocument("https://git-scm.com/downloads")
            System.exit(-1)
            return
        }
        if (isWindows && gitGetCredentialHelper().isBlank()) gitSetWincred()
        if (isMac && gitGetCredentialHelper().isBlank()) gitSetKeychain()

        // TODO: move this?
        credentialService.credentialHandler = { CredentialsDialog(it, stage).showAndWait() }

        initSettings()
        initWindow()
        initScaling()

        stage.show()

        scheduledPool.scheduleAtFixedRate({ if (!stage.isFocused && !state.isModal.get()) Platform.runLater { fireEvent() } }, 0, 10, TimeUnit.SECONDS)
    }

    override fun stop() = settings.save()

    private fun initSettings() {
        settings.addOnSave {
            it["window"] = json {
                +("x" to stage.x)
                +("y" to stage.y)
                +("width" to stage.width)
                +("height" to stage.height)
                +("maximized" to stage.isMaximized)
                +("fullscreen" to stage.isFullScreen)
            }
        }
        settings.load {
            it["window"]?.let {
                stage.x = it.getDouble("x")!!
                stage.y = it.getDouble("y")!!
                stage.width = it.getDouble("width")!!.takeIf { it > 1.0 } ?: 1280.0
                stage.height = it.getDouble("height")!!.takeIf { it > 1.0 } ?: 800.0
                stage.isMaximized = it.getBoolean("maximized")!!
                stage.isFullScreen = it.getBoolean("fullscreen")!!
            }
        }
    }

    private fun initWindow() {
        stage.focusedProperty().addListener { _, _, it -> if (it) state.isModal.takeIf { it.get() }?.set(false) ?: fireEvent() }
        stage.scene = Scene(GitView())
        stage.scene.stylesheets += "default.css".asResource()
        stage.icons += Image("icon.png".asResource())
        stage.titleProperty().bind(Bindings.createStringBinding(Callable { updateTitle() },
                repositoryService.activeRepository,
                mergeService.isMerging,
                rebaseService.isRebasing,
                rebaseService.rebaseNext,
                rebaseService.rebaseLast))
    }

    private fun initScaling() {
        // TODO
    }

    private fun updateTitle(): String {
        val repository = repositoryService.activeRepository.get()?.let {
            val path = if (isMac) it.path.stripHome() else it.path
            val rebase = if (rebaseService.isRebasing.get()) "REBASING ${rebaseService.rebaseNext.get()}/${rebaseService.rebaseLast.get()} " else ""
            val merge = if (mergeService.isMerging.get()) "MERGING " else ""
            "${it.shortPath} [$path] $merge$rebase\u2012 "
        }
        return "${repository ?: ""}TinyGit ${javaClass.`package`.implementationVersion ?: ""}"
    }

}
