package hamburg.remme.tinygit

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

/**
 * Class for loading and saving [Json] from/to a YAML file located in the [homeDir].
 */
class Settings {

    private val yaml = Yaml(Representer().apply { propertyUtils.setSkipMissingProperties(true) },
            DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
    private val settingsFile = "$homeDir/.tinygit".asPath()
    private val suppliers = mutableListOf<(Json) -> Unit>()
    private var settings: Json? = null

    /**
     * Adds a callback that is called on [save].
     */
    fun addOnSave(block: (Json) -> Unit) {
        suppliers += block
    }

    /**
     * Loads the settings from a file or cache and calls the given [block] with it.
     * May not be called if the settings are `null`.
     *
     * @todo: remove migration step
     */
    @Suppress("UNCHECKED_CAST")
    fun load(block: (Json) -> Unit) {
        if (settingsFile.exists() && settings == null) {
            settings = settingsFile.read()
                    .let {
                        if (it.startsWith("!!")) it.lines().drop(1).joinToString("\n") else it
                    }.let {
                        Json(yaml.load(it) as Map<String, *>)
                    }
        }
        settings?.let(block)
    }

    /**
     * Saves the settings to a YAML file in the [homeDir].
     */
    fun save() {
        if (settings == null) settings = Json()
        suppliers.forEach { it(settings!!) }
        settingsFile.write(yaml.dump(settings))
    }

}
