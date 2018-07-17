package hamburg.remme.tinygit

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.text.Font
import javafx.stage.Stage
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

fun main(args: Array<String>) {
    Application.launch(TinyGitApplication::class.java, *args)
}

private const val MAIN_STYLESHEET = "/generated/main.css"
private const val MAIN_FXML = "/fxml/main.fxml"
private const val FONT_SIZE = 13.0
private const val FONT_SIZE_PROPERTY = "com.sun.javafx.fontSize"
private const val FONT_ROBOTO_REGULAR = "font/Roboto-Regular.ttf"
private const val FONT_ROBOTO_BOLD = "font/Roboto-Bold.ttf"
private const val FONT_ROBOTO_LIGHT = "font/Roboto-Light.ttf"
private const val FONT_LIBERATION_MONO = "font/LiberationMono-Regular.ttf"
private const val FONTAWESOME = "font/fa-solid-900.ttf"
private const val FONTAWESOME_BRANDS = "font/fa-brands-400.ttf"

@SpringBootApplication
class TinyGitApplication : Application() {

    private lateinit var springContext: ConfigurableApplicationContext
    private lateinit var root: Parent

    override fun init() {
        springContext = SpringApplication.run(TinyGitApplication::class.java)
        initFX()
        initFXML()
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = "TinyGit"
        primaryStage.scene = Scene(root, 800.0, 600.0)
        primaryStage.show()
    }

    override fun stop() {
        springContext.stop()
    }

    private fun initFX() {
        // Will load needed fonts and set the font size depending on the OS
        // This might be a solution to the DPI issues on Linux, e.g. Ubuntu
        System.setProperty(FONT_SIZE_PROPERTY, FONT_SIZE.toString())
        Font.loadFont(javaClass.getResourceAsStream(FONT_ROBOTO_REGULAR), FONT_SIZE)
        Font.loadFont(javaClass.getResourceAsStream(FONT_ROBOTO_BOLD), FONT_SIZE)
        Font.loadFont(javaClass.getResourceAsStream(FONT_ROBOTO_LIGHT), FONT_SIZE)
        Font.loadFont(javaClass.getResourceAsStream(FONT_LIBERATION_MONO), FONT_SIZE)
        Font.loadFont(javaClass.getResourceAsStream(FONTAWESOME), FONT_SIZE)
        Font.loadFont(javaClass.getResourceAsStream(FONTAWESOME_BRANDS), FONT_SIZE)

        Application.setUserAgentStylesheet(javaClass.getResource(MAIN_STYLESHEET).toExternalForm())
    }

    private fun initFXML() {
        val fxmlLoader = FXMLLoader(javaClass.getResource(MAIN_FXML))
        fxmlLoader.setControllerFactory { springContext.getBean(it) }
        root = fxmlLoader.load()
    }

}
