package hamburg.remme.tinygit

import hamburg.remme.tinygit.gui.GitView
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.StashService
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import java.util.Locale
import java.util.concurrent.Callable

// TODO: the great clean-up tbd
// clean-up nested collection methods
// cache stuff in nested collection methods, like find {}
// clean-up nested let {} for map {}
// instead of also {} and apply {}, use onEach {} at the end of a statement chain
// boolean properties (and parameters) should start with 'is'
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
        BranchService
        DivergenceService
        MergeService
        RebaseService
        StashService

        Settings.setRepositories { State.getAllRepositories() }
        Settings.setWindow { Settings.WindowSettings(stage.x, stage.y, stage.width, stage.height, stage.isMaximized, stage.isFullScreen) }
        Settings.load {
            State.setRepositories(it.repositories)
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
                else State.fireRefresh(stage)
            }
        }
        stage.scene = Scene(GitView())
        stage.scene.stylesheets += "default.css".asResource()
        stage.icons += Image("icon.png".asResource())
        stage.titleProperty().bind(Bindings.createStringBinding(Callable { updateTitle() },
                State.selectedRepository, State.isMerging, State.isRebasing, State.rebaseNext, State.rebaseLast))
        stage.show()
    }

    override fun stop() = Settings.save()

    private fun updateTitle(): String {
        val repository = State.selectedRepository.get()?.let {
            val rebase = if (State.isRebasing.get()) "REBASING ${State.rebaseNext.get()}/${State.rebaseLast.get()} " else ""
            val merge = if (State.isMerging.get()) "MERGING " else ""
            "${it.shortPath} [$it] $merge$rebase\u2012 "
        } ?: ""
        return "$repository$title"
    }

}
