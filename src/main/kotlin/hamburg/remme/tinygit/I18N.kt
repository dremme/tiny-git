package hamburg.remme.tinygit

import hamburg.remme.tinygit.gui.builder.hbox
import javafx.scene.Node
import javafx.scene.text.Text
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.ChoiceFormat
import java.text.MessageFormat
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle


object I18N {

    private val bundle = ResourceBundle.getBundle("hamburg.remme.tinygit.message", UTF8Support())

    /**
     * Get the message property [key] with [icon] as `{0}`.
     *
     * @return a node containing the message and the [icon]
     *
     * @throws NullPointerException if [key] is not present in the message bundle
     */
    operator fun get(key: String, icon: Node): Node = hbox {
        val parts = bundle[key].split("\\{\\d+}".toRegex())
        +Text(parts[0])
        +icon
        +Text(parts[1])
    }

    /**
     * Get the message property [key].
     *
     * @throws NullPointerException if [key] is not present in the message bundle
     */
    operator fun get(key: String) = bundle[key]

    /**
     * Get the message property [key] formatted with [count] as choice and `{0}`.
     *
     * @throws NullPointerException if [key] is not present in the message bundle
     */
    operator fun get(key: String, count: Number) = format(choose(bundle[key], count), count)

    /**
     * Get the message property [key] formatted with [count] as choice and `{0}` and [args] starting with `{1}`.
     *
     * @throws NullPointerException if [key] is not present in the message bundle
     */
    operator fun get(key: String, count: Number, vararg args: Any) = format(choose(bundle[key], count), count, *args)

    /**
     * Get the message property [key] formatted with [args].
     *
     * @throws NullPointerException if [key] is not present in the message bundle
     */
    operator fun get(key: String, vararg args: Any) = format(bundle[key], *args)

    private fun format(template: String, vararg args: Any) = MessageFormat.format(template.replace("'", "''"), *args)!!

    private fun choose(template: String, count: Number) = ChoiceFormat(template).format(count)!!

    private operator fun ResourceBundle.get(key: String) = getString(key)!!

    // Copy of default implementation
    private class UTF8Support : ResourceBundle.Control() {

        override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? {
            val bundleName = toBundleName(baseName, locale)
            val resourceName = toResourceName(bundleName, "properties")
            val stream = if (reload) {
                loader.getResource(resourceName).let {
                    it.openConnection().let {
                        it.useCaches = false
                        it.getInputStream()
                    }
                }
            } else {
                loader.getResourceAsStream(resourceName)
            }
            return stream.use { PropertyResourceBundle(InputStreamReader(it, StandardCharsets.UTF_8)) }
        }

    }

}
