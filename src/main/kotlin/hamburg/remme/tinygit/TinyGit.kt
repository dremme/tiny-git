package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitDetailsService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.CommitService
import hamburg.remme.tinygit.domain.service.DiffService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.RemoteService
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StashService
import hamburg.remme.tinygit.domain.service.WorkingCopyService
import hamburg.remme.tinygit.git.gitIsInstalled
import hamburg.remme.tinygit.git.gitVersion
import hamburg.remme.tinygit.gui.GitView
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.fatalAlert
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import java.util.Locale
import java.util.concurrent.Callable

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

        private lateinit var tinygit: Application

        fun show(uri: String) {
            tinygit.hostServices.showDocument(uri)
        }

    }

    private val title = "TinyGit ${javaClass.`package`.implementationVersion ?: ""}"

    override fun start(stage: Stage) {
        tinygit = this

        // We terminate here because technical requirements for TinyGit aren't met
        if (!gitIsInstalled()) {
            fatalAlert("Git Error", "Git is not installed or not in PATH.")
            show("https://git-scm.com/downloads")
            System.exit(-1)
            return
        }
        if (gitVersion().major < 2) {
            fatalAlert("Git Error", "The installed Git client is out of date.\nPlease install the newest version.")
            show("https://git-scm.com/downloads")
            System.exit(-1)
            return
        }

        RepositoryService
        CommitLogService.timeoutHandler = {
            errorAlert(stage, "Cannot Fetch From Remote",
                    "Please check the repository settings.\nCredentials or proxy settings may have changed.")
        }
        State.addListeners(RemoteService)
        State.addListeners(BranchService)
        State.addListeners(CommitLogService)
        State.addListeners(WorkingCopyService)
        State.addListeners(DivergenceService)
        State.addListeners(MergeService)
        State.addListeners(RebaseService)
        State.addListeners(StashService)
        State.addListeners(DiffService)
        State.addListeners(CommitDetailsService)
        State.addListeners(CommitService)

        Settings.setWindow { Settings.WindowSettings(stage.x, stage.y, stage.width, stage.height, stage.isMaximized, stage.isFullScreen) }
        Settings.load {
            stage.x = it.window.x
            stage.y = it.window.y
            stage.width = it.window.width.takeIf { it > 1.0 } ?: 1280.0
            stage.height = it.window.height.takeIf { it > 1.0 } ?: 800.0
            stage.isMaximized = it.window.maximized
            stage.isFullScreen = it.window.fullscreen
        }

        stage.focusedProperty().addListener { _, _, it ->
            if (it) {
                if (State.isModal.get()) State.isModal.set(false)
                else State.fireRefresh()
            }
        }
        stage.scene = Scene(GitView())
        stage.scene.stylesheets += "default.css".asResource()
        stage.icons += Image("icon.png".asResource())
        stage.titleProperty().bind(Bindings.createStringBinding(Callable { updateTitle() },
                RepositoryService.activeRepository, MergeService.isMerging, RebaseService.isRebasing, RebaseService.rebaseNext, RebaseService.rebaseLast))
        stage.show()
    }

    override fun stop() = Settings.save()

    private fun updateTitle(): String {
        val repository = RepositoryService.activeRepository.get()?.let {
            val rebase = if (RebaseService.isRebasing.get()) "REBASING ${RebaseService.rebaseNext.get()}/${RebaseService.rebaseLast.get()} " else ""
            val merge = if (MergeService.isMerging.get()) "MERGING " else ""
            "${it.shortPath} [$it] $merge$rebase\u2012 "
        } ?: ""
        return "$repository$title"
    }

}
