package hamburg.remme.tinygit

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

class Settings {

    private val yaml = Yaml(Representer().apply { propertyUtils.setSkipMissingProperties(true) },
            DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
    private val settingsFile = "$homeDir/.tinygit".asPath()
    private val suppliers = mutableListOf<(Json) -> Unit>()
    private var settings: Json? = null

    fun addOnSave(block: (Json) -> Unit) {
        suppliers += block
    }

    @Suppress("UNCHECKED_CAST")
    fun load(block: (Json) -> Unit) {
        if (settingsFile.exists() && settings == null) {
            settings = settingsFile.read()
                    .let {
                        // TODO: needed for migration
                        if (it.startsWith("!!")) it.lines().drop(1).joinToString("\n") else it
                    }.let {
                        Json(yaml.load(it) as Map<String, *>)
                    }
        }
        settings?.let(block)
    }

    fun save() {
        if (settings == null) settings = Json()
        suppliers.forEach { it.invoke(settings!!) }
        settingsFile.write(yaml.dump(settings))
    }

}
