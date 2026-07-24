package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.service.CredentialService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.RepositoryService
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
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.Window
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Callable
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import kotlin.system.exitProcess

/**
 * Will launch [TinyGit] with the given [args].
 * Will also set the locale to [Locale.ROOT] and font size to [fontSize] at the moment.
 *
 * @todo: fix DPI issues for Linux
 * @todo: use functions like [List.mapTo] to save some memory
 */
fun main(args: Array<String>) {
    Locale.setDefault(Locale.ROOT)
    AppFonts.install()
    Application.launch(TinyGit::class.java, *args)
}

/**
 * A very small, fast and portable Git GUI. Repositories can be configured with credentials, SSH keys and proxies
 * separately. No need to toggle the proxy setting of `.gitconfig`.
 *
 * The application is structured into sections or views, which are getting their data from services. The class
 * [GitView] is the root of the main window and initialized menu and tool bars.
 *
 * @see Settings
 * @see State
 *
 * @author Dennis Remme (dennis@remme.hamburg)
 */
class TinyGit : Application() {

    /**
     * A singleton holding all the needed services and application states. It uses component scan to gather classes
     * and instantiate them. Dependency injection is supported via constructor injection.
     *
     * Also some convenience functions related to the [TinyGit] class.
     *
     * @todo: really cast the main [Stage] to [Window]?
     *
     * @see Service
     * @see Refreshable
     */
    companion object {

        /**
         * @see [TinyGit.get]
         */
        val servicesUnmodifiable: Map<KClass<*>, Any> get() = Collections.unmodifiableMap(services)
        /**
         * The primary window of the application
         */
        val window: Window get() = stage
        private val services = createDependencyMap(scanAnnotation<Service>())
        private val listeners = mutableListOf<(Repository) -> Unit>()
        private val settings = get<Settings>()
        private val credentialService = get<CredentialService>()
        private val repositoryService = get<RepositoryService>()
        private val mergeService = get<MergeService>()
        private val rebaseService = get<RebaseService>()
        private val state = get<State>()
        private lateinit var application: Application
        private lateinit var stage: Stage

        init {
            services.mapNotNull { (_, it) -> it as? Refreshable }
                    .forEach { refreshable ->
                        repositoryService.activeRepository.addListener { _, _, it ->
                            it?.let { refreshable.onRepositoryChanged(it) }
                                    ?: refreshable.onRepositoryDeselected()
                        }
                        addListener { refreshable.onRefresh(it) }
                    }
        }

        /**
         * Returns a singleton-like instance of the given [KClass]. This may only succeed if the class has been
         * instantiated by the component scan.
         *
         * @throws IllegalArgumentException if there is no instance of type [KClass]
         * @throws ClassCastException if the instance is not of type [KClass]
         *
         * @see Service
         */
        inline fun <reified T> get() = servicesUnmodifiable[T::class] as? T
                ?: throw IllegalArgumentException("No instance of type '${T::class.java.name}' available.")

        /**
         * Adds a refresh listener. Will only be called if the active repository is not `null`.
         *
         * @todo: find a way to get rid of this
         */
        fun addListener(block: (Repository) -> Unit) {
            listeners += block
        }

        /**
         * Fires a refresh event. Will only trigger if the active repository is not `null`.
         */
        fun fireEvent() = Platform.runLater {
            repositoryService.activeRepository.get()?.let { repository -> listeners.forEach { it(repository) } }
        }

        /**
         * Runs a parallel task in the [cachedPool] and will also block the application from UI events.
         * A [message] is shown as application overlay.
         *
         * @see [Task.execute]
         */
        fun run(message: String, task: Task<*>) {
            task.setOnSucceeded { state.runningProcesses.dec() }
            task.setOnCancelled { state.runningProcesses.dec() }
            task.setOnFailed { state.runningProcesses.dec() }
            state.processText.set(message)
            state.runningProcesses.inc()
            task.execute()
        }

        /**
         * Opens the default web browser with the given [url].
         */
        fun showDocument(url: String) = application.hostServices.showDocument(url)

    }

    init {
        TinyGit.application = this
        // Prefer classpath CSS (compiled from SCSS) as the full user-agent stylesheet
        val stylesheet = if (isMac) "/css/main-mac.css" else "/css/main-windows.css"
        Application.setUserAgentStylesheet(stylesheet.asResource())
    }

    override fun start(primaryStage: Stage) {
        TinyGit.stage = primaryStage

        // We terminate here because technical requirements for TinyGit aren't met
        if (isMac || isLinux) gitIsInstalled() // UNIX workaround
        if (!gitIsInstalled()) {
            fatalAlert(I18N["error.gitError"], I18N["error.gitNotInstalled"])
            showDocument("https://git-scm.com/downloads")
            exitProcess(-1)
            return
        }
        if (gitVersion().major < 2) {
            fatalAlert(I18N["error.gitError"], I18N["error.gitOutOfDate"])
            showDocument("https://git-scm.com/downloads")
            exitProcess(-1)
            return
        }
        if (isWindows && gitGetCredentialHelper().isBlank()) gitSetWincred()
        if (isMac && gitGetCredentialHelper().isBlank()) gitSetKeychain()

        initHandlers()
        initWindow()
        initSettings() // after scene exists so width/height restore is not overwritten
        showWindow()
    }

    override fun stop() = settings.save()

    private fun initHandlers() {
        credentialService.credentialHandler = { CredentialsDialog(it, stage).showAndWait() }
    }

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
            it["window"]?.let { window ->
                applyWindowSettings(
                        x = window.getDouble("x"),
                        y = window.getDouble("y"),
                        width = window.getDouble("width"),
                        height = window.getDouble("height"),
                        maximized = window.getBoolean("maximized") == true,
                        fullscreen = window.getBoolean("fullscreen") == true
                )
            }
            // else keep defaults from initWindow()
        }
    }

    private fun initWindow() {
        stage.focusedProperty().addListener { _, _, it -> if (it) state.isModal.takeIf { it.get() }?.set(false) ?: fireEvent() }
        val bounds = primaryVisualBounds()
        val (defaultW, defaultH) = defaultWindowSize(bounds)
        // Explicit size: Scene must not adopt content preferred size (can be huge from MAX_VALUE prefs).
        stage.scene = Scene(GitView(), defaultW, defaultH)
        stage.width = defaultW
        stage.height = defaultH
        stage.minWidth = 640.0
        stage.minHeight = 480.0
        stage.icons += Image("icon.png".asResource())
        stage.titleProperty().bind(Bindings.createStringBinding({ updateTitle() },
                repositoryService.activeRepository,
                mergeService.isMerging,
                rebaseService.isRebasing,
                rebaseService.rebaseNext,
                rebaseService.rebaseLast))
        // Keep chrome on whole pixels while the user resizes/moves the window.
        stage.xProperty().addListener { _, _, v -> if (v.toDouble() % 1.0 != 0.0) stage.x = v.toDouble().roundToInt().toDouble() }
        stage.yProperty().addListener { _, _, v -> if (v.toDouble() % 1.0 != 0.0) stage.y = v.toDouble().roundToInt().toDouble() }
    }

    private fun showWindow() {
        stage.show()
        // Re-clamp after show (platform may adjust bounds) and snap to whole pixels.
        clampStageToScreen()
        stage.x = stage.x.roundToInt().toDouble()
        stage.y = stage.y.roundToInt().toDouble()
        schedule(10000) { if (!stage.isFocused && !state.isModal.get()) fireEvent() }
    }

    private fun primaryVisualBounds(): Rectangle2D = Screen.getPrimary().visualBounds

    private fun defaultWindowSize(bounds: Rectangle2D = primaryVisualBounds()): Pair<Double, Double> {
        val width = min(1280.0, bounds.width * 0.9).roundToInt().toDouble().coerceAtLeast(640.0)
        val height = min(800.0, bounds.height * 0.9).roundToInt().toDouble().coerceAtLeast(480.0)
        return width to height
    }

    private fun applyWindowSettings(
            x: Double?,
            y: Double?,
            width: Double?,
            height: Double?,
            maximized: Boolean,
            fullscreen: Boolean
    ) {
        val bounds = primaryVisualBounds()
        val (defaultW, defaultH) = defaultWindowSize(bounds)
        val w = (width?.takeIf { it > 1.0 } ?: defaultW)
                .roundToInt().toDouble()
                .coerceIn(640.0, bounds.width)
        val h = (height?.takeIf { it > 1.0 } ?: defaultH)
                .roundToInt().toDouble()
                .coerceIn(480.0, bounds.height)
        val maxX = bounds.minX + bounds.width - w
        val maxY = bounds.minY + bounds.height - h
        val px = (x ?: (bounds.minX + (bounds.width - w) / 2)).roundToInt().toDouble()
                .coerceIn(bounds.minX, maxX.coerceAtLeast(bounds.minX))
        val py = (y ?: (bounds.minY + (bounds.height - h) / 2)).roundToInt().toDouble()
                .coerceIn(bounds.minY, maxY.coerceAtLeast(bounds.minY))
        stage.width = w
        stage.height = h
        stage.x = px
        stage.y = py
        stage.isMaximized = maximized
        stage.isFullScreen = fullscreen
    }

    private fun clampStageToScreen() {
        if (stage.isFullScreen || stage.isMaximized) return
        val bounds = primaryVisualBounds()
        if (stage.width > bounds.width) stage.width = bounds.width
        if (stage.height > bounds.height) stage.height = bounds.height
        val maxX = bounds.minX + bounds.width - stage.width
        val maxY = bounds.minY + bounds.height - stage.height
        if (stage.x < bounds.minX || stage.x > maxX) {
            stage.x = stage.x.coerceIn(bounds.minX, maxX.coerceAtLeast(bounds.minX))
        }
        if (stage.y < bounds.minY || stage.y > maxY) {
            stage.y = stage.y.coerceIn(bounds.minY, maxY.coerceAtLeast(bounds.minY))
        }
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
