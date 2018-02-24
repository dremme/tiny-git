package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.Repository
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

// TODO: refactor settings to use a lightweight json class
class Settings {

    private val yaml = Yaml(Representer().apply { propertyUtils.setSkipMissingProperties(true) },
            DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
    private val settingsFile = "${System.getProperty("user.home")}/.tinygit".asPath()
    private val suppliers = arrayOfNulls<() -> Any?>(8)
    private var settings: LocalSettings? = null

    fun load(block: (LocalSettings) -> Unit) {
        if (settingsFile.exists() && settings == null) {
            try {
                settings = yaml.loadAs(settingsFile.read(), LocalSettings::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        settings?.let(block)
    }

    fun save() {
        settingsFile.write(yaml.dump(LocalSettings(
                suppliers.invoke(0),
                suppliers.invoke(1),
                suppliers.invoke(2),
                suppliers.invoke(3),
                suppliers.invoke(4),
                suppliers.invoke(5),
                suppliers.invoke(6),
                suppliers.invoke(7))))
    }

    fun setRepositories(supplier: () -> List<Repository>) {
        suppliers[0] = supplier
    }

    fun setRepositorySelection(supplier: () -> Repository?) {
        suppliers[1] = supplier
    }

    fun setTree(supplier: () -> List<TreeItem>) {
        suppliers[2] = supplier
    }

    fun setWindow(supplier: () -> WindowSettings) {
        suppliers[3] = supplier
    }

    fun setTabSelection(supplier: () -> Int) {
        suppliers[4] = supplier
    }

    fun setUsedNames(supplier: () -> List<String>) {
        suppliers[5] = supplier
    }

    fun setUsedEmails(supplier: () -> List<String>) {
        suppliers[6] = supplier
    }

    fun setUsedProxies(supplier: () -> List<String>) {
        suppliers[7] = supplier
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Array<(() -> Any?)?>.invoke(index: Int): T {
        return this[index]?.invoke() as? T ?: throw RuntimeException("Missing supplier for setting $index")
    }

    class LocalSettings(var repositories: List<Repository> = emptyList(),
                        var repositorySelection: Repository? = null,
                        var tree: List<TreeItem> = emptyList(),
                        var window: WindowSettings = WindowSettings(),
                        var tabSelection: Int = 0,
                        var usedNames: List<String> = emptyList(),
                        var usedEmails: List<String> = emptyList(),
                        var usedProxies: List<String> = emptyList())

    class TreeItem(var index: Int = 0,
                   var expanded: Boolean = false)

    class WindowSettings(var x: Double = 0.0,
                         var y: Double = 0.0,
                         var width: Double = 0.0,
                         var height: Double = 0.0,
                         var maximized: Boolean = false,
                         var fullscreen: Boolean = false)

}
