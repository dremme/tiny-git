package hamburg.remme.tinygit

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.StopWatch
import java.util.ResourceBundle

fun main(vararg args: String) {
    Application.launch(TinyGitApplication::class.java, *args)
}

private const val MAIN_STYLESHEET = "/generated/main.css"
private const val MAIN_FXML = "/fxml/main.fxml"
private const val MAIN_ICON = "/icon.png"
private const val FONT_SIZE = 13.0
private const val FONT_SIZE_PROPERTY = "com.sun.javafx.fontSize"
private const val FONT_ROBOTO_REGULAR = "/font/Roboto-Regular.ttf"
private const val FONT_ROBOTO_BOLD = "/font/Roboto-Bold.ttf"
private const val FONT_ROBOTO_LIGHT = "/font/Roboto-Light.ttf"
private const val FONT_LIBERATION_MONO = "/font/LiberationMono-Regular.ttf"
private const val FONTAWESOME = "/font/fa-solid-900.ttf"
private const val FONTAWESOME_BRANDS = "/font/fa-brands-400.ttf"

/**
 * A JavaFX and Spring Boot application. Complete madness.
 */
@EnableCaching // TODO: move?
@SpringBootApplication class TinyGitApplication : Application() {

    private val log = logger<TinyGitApplication>()
    private val stopWatch = StopWatch()
    private lateinit var springContext: ConfigurableApplicationContext
    private lateinit var root: Parent

    /**
     * Called by [Application.launch]. Will start Spring and initialize the scene.
     */
    override fun init() {
        // Run Spring Boot application with auto-config and detection
        springContext = SpringApplication.run(TinyGitApplication::class.java)

        log.info("Starting JavaFX application")
        stopWatch.start()

        initFX()
        initFXML()
    }

    /**
     * Will open the primary [Stage].
     *
     * @param primaryStage the stage given by the JavaFX launcher.
     */
    override fun start(primaryStage: Stage) {
        primaryStage.icons += Image(MAIN_ICON.openStream())
        primaryStage.title = "TinyGit"
        primaryStage.scene = Scene(root, 800.0, 600.0)
        primaryStage.show()

        stopWatch.stop()
        log.info("Started JavaFX application in {} seconds", stopWatch.lastTaskTimeMillis / 1000.0)
    }

    /**
     * Will tear down the Spring application.
     */
    override fun stop() {
        // Tear down Spring Boot
        springContext.stop()
    }

    private fun initFX() {
        // TODO: uncomment after prototyping phase
        // Will load needed fonts and set the font size depending on the OS to reduce lag during runtime
        // We never load fonts via CSS
        // System.setProperty(FONT_SIZE_PROPERTY, FONT_SIZE.toString())
        // Font.loadFont(resourceStream(FONT_ROBOTO_REGULAR), FONT_SIZE)
        // Font.loadFont(resourceStream(FONT_ROBOTO_BOLD), FONT_SIZE)
        // Font.loadFont(resourceStream(FONT_ROBOTO_LIGHT), FONT_SIZE)
        // Font.loadFont(resourceStream(FONT_LIBERATION_MONO), FONT_SIZE)
        // Font.loadFont(resourceStream(FONTAWESOME), FONT_SIZE)
        // Font.loadFont(resourceStream(FONTAWESOME_BRANDS), FONT_SIZE)
        // Will load the custom CSS stylesheet. Must be called before any scene is initialized.
        // This will prevent modena.css to be loaded at all
        // Application.setUserAgentStylesheet(resourceString(MAIN_STYLESHEET))
    }

    private fun initFXML() {
        val fxmlLoader = FXMLLoader(MAIN_FXML.toURL())
        fxmlLoader.resources = springContext.getBean(ResourceBundle::class.java)
        fxmlLoader.setControllerFactory { springContext.getBean(it) }
        root = fxmlLoader.load()
    }

}
