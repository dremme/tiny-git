package hamburg.remme.tinygit

import javafx.scene.text.Font

/**
 * Installs embedded fonts and Prism text settings before the JavaFX toolkit
 * finishes bringing up the first scene.
 *
 * Dark UI + LCD subpixel AA often produces color fringing and soft edges on
 * Windows; gray AA is the crisper default for this theme.
 */
object AppFonts {
    /** Design base size in CSS pixels (Windows 12 / macOS 13). */
    val baseSizePx: Double = if (isMac) 13.0 else 12.0

    private val faces =
        listOf(
            "font/Inter-Regular.ttf",
            "font/Inter-Bold.ttf",
            "font/Inter-Light.ttf",
            "font/fa-solid-900.otf",
            "font/fa-brands-400.otf",
        )

    fun install() {
        // Must be set before the toolkit creates the first Scene / glass window.
        System.setProperty("prism.lcdtext", "false")
        System.setProperty("prism.forceIntegerRenderScale", "true")

        faces.forEach { path ->
            val url = path.asResource()
            val font =
                Font.loadFont(url, baseSizePx)
                    ?: error("Failed to load embedded font: $path ($url)")
            // Touch the family so the CSS engine can resolve it immediately.
            Font.font(font.family, baseSizePx)
        }
    }
}
