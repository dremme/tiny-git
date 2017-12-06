package hamburg.remme.tinygit

import hamburg.remme.tinygit.gui.GitView
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import java.util.Locale

fun main(args: Array<String>) {
    Locale.setDefault(Locale.ROOT)
    Font.loadFont("font/Roboto-Regular.ttf".asResource(), 13.0)
    Font.loadFont("font/Roboto-Bold.ttf".asResource(), 13.0)
    Font.loadFont("font/LiberationMono-Regular.ttf".asResource(), 12.0)
    Font.loadFont("font/fontawesome-webfont.ttf".asResource(), 14.0)
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

        Settings.setRepository { State.repositories }
        Settings.load { State.repositories.setAll(repositories) }

        stage.focusedProperty().addListener { _, _, it ->
            if (it) {
                if (State.modalVisible.get()) State.modalVisible.set(false)
                else State.fireRefresh()
            }
        }
        stage.scene = Scene(GitView())
        stage.scene.stylesheets += "default.css".asResource()
        stage.title = "TinyGit ${javaClass.`package`.implementationVersion ?: ""}"
        stage.icons += Image("icon.png".asResource())
        stage.width = 1280.0
        stage.height = 800.0
        stage.isMaximized = true
        stage.show()
    }

    override fun stop() {
        State.stop()
        Settings.save()
    }

}
