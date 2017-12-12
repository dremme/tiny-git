package hamburg.remme.tinygit

import hamburg.remme.tinygit.gui.GitView
import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import java.util.Locale
import java.util.concurrent.Callable

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

    override fun start(stage: Stage) {
        tinygit = this

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
                if (State.modalVisible.get()) State.modalVisible.set(false)
                else State.fireRefresh()
            }
        }
        stage.scene = Scene(GitView())
        stage.scene.stylesheets += "default.css".asResource()
        stage.icons += Image("icon.png".asResource())
        stage.titleProperty().bind(Bindings.createStringBinding(
                Callable {
                    val repo = State.selectedRepository.get()
                    "${repo?.let { "${it.shortPath} [$it] \u2012 " } ?: ""}TinyGit ${javaClass.`package`.implementationVersion ?: ""}"
                },
                State.selectedRepository))
        stage.show()
    }

    override fun stop() {
        State.stop()
        Settings.save()
    }

}
