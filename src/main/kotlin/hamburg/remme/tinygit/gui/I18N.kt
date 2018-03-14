package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.gui.builder.hbox
import javafx.scene.Node
import javafx.scene.text.Text
import java.text.ChoiceFormat
import java.text.MessageFormat
import java.util.ResourceBundle

object I18N {

    private val bundle = ResourceBundle.getBundle("hamburg.remme.tinygit.message")

    operator fun get(key: String, icon: Node): Node = hbox {
        val parts = bundle[key].split("\\{\\d+}".toRegex())
        +Text(parts[0])
        +icon
        +Text(parts[1])
    }

    operator fun get(key: String) = bundle[key]

    operator fun get(key: String, vararg args: Any) = format(bundle[key], *args)

    operator fun get(key: String, count: Int) = format(choose(bundle[key], count), count)

    operator fun get(key: String, count: Int, vararg args: Any) = format(choose(bundle[key], count), *args)

    private fun format(template: String, vararg args: Any) = MessageFormat.format(template.replace("'", "''"), *args)!!

    private fun choose(template: String, count: Int) = ChoiceFormat(template).format(count)!!

    private operator fun ResourceBundle.get(key: String) = getString(key)!!

}
