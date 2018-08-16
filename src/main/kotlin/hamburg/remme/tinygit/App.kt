package hamburg.remme.tinygit

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.stage.Window
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.get
import org.springframework.util.StopWatch
import java.util.ResourceBundle

fun main(vararg args: String) {
    Application.launch(GitAnalytics::class.java, *args)
}

/**
 * A JavaFX and Spring Boot application. Complete madness.
 */
@EnableCaching // TODO: move?
@SpringBootApplication class GitAnalytics : Application() {

    private val log = logger<GitAnalytics>()
    private val stopWatch = StopWatch()
    private lateinit var springContext: ConfigurableApplicationContext
    private lateinit var root: Parent

    /**
     * The application context holding objects like the primary [Window].
     */
    @Bean fun context(): Context {
        return Context()
    }

    /**
     * Called by [Application.launch]. Will start Spring and initialize the scene.
     */
    override fun init() {
        // Run Spring Boot application with auto-config and detection
        springContext = SpringApplication.run(GitAnalytics::class.java)

        log.info("Starting JavaFX application")
        stopWatch.start()

        initFX()
        initFXML()

        stopWatch.stop()
        log.info("Started JavaFX application in {} seconds", stopWatch.lastTaskTimeMillis / 1000.0)
    }

    /**
     * Will open the primary [Stage].
     *
     * @param primaryStage the stage given by the JavaFX launcher.
     */
    override fun start(primaryStage: Stage) {
        initStage(primaryStage)
        primaryStage.show()
    }

    /**
     * Will tear down the Spring application.
     */
    override fun stop() {
        springContext.stop()
    }

    private fun initFX() {
        // Will load needed fonts and set the font size depending on the OS to reduce lag during runtime
        // We never load fonts via CSS
        System.setProperty("com.sun.javafx.fontSize", springContext.environment["javafx.fontSize"])
        springContext.environment["javafx.fonts"].split(";").forEach(this::initFont)
        // Will load the custom CSS stylesheet. Must be called before any scene is initialized.
        // This will prevent modena.css to be loaded at all
        // TODO: uncomment when ready
//        Application.setUserAgentStylesheet(springContext.environment["javafx.css"].toExternal())
    }

    private fun initFont(fontPath: String) {
        Font.loadFont(fontPath.openStream(), Font.getDefault().size)
    }

    private fun initFXML() {
        val fxmlLoader = FXMLLoader(springContext.environment["javafx.fxml"].toURL())
        fxmlLoader.resources = springContext.getBean(ResourceBundle::class.java)
        fxmlLoader.setControllerFactory { springContext.getBean(it) }
        root = fxmlLoader.load()
    }

    private fun initStage(primaryStage: Stage) {
        val context = springContext.getBean(Context::class.java)
        context.window = primaryStage

        primaryStage.title = springContext.environment["javafx.title"]
        primaryStage.icons += Image(springContext.environment["javafx.icon"].openStream())
        primaryStage.scene = Scene(root, 800.0, 600.0)
    }

}
