package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.Repository
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

class Settings {

    private val yaml = Yaml(Representer().apply { propertyUtils.setSkipMissingProperties(true) },
            DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
    private val settingsFile = "${System.getProperty("user.home")}/.tinygit".asPath()
    private val suppliers: MutableMap<Category, () -> Any?> = mutableMapOf()
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
                getCategory(Category.REPOSITORIES),
                getCategory(Category.REPO_SELECTION),
                getCategory(Category.TREE),
                getCategory(Category.WINDOW),
                getCategory(Category.TAB_SELECTION),
                getCategory(Category.USED_PROXIES))))
    }

    fun setRepositories(supplier: () -> List<Repository>) {
        suppliers[Category.REPOSITORIES] = supplier
    }

    fun setRepositorySelection(supplier: () -> Repository?) {
        suppliers[Category.REPO_SELECTION] = supplier
    }

    fun setTree(supplier: () -> List<TreeItem>) {
        suppliers[Category.TREE] = supplier
    }

    fun setWindow(supplier: () -> WindowSettings) {
        suppliers[Category.WINDOW] = supplier
    }

    fun setTabSelection(supplier: () -> Int) {
        suppliers[Category.TAB_SELECTION] = supplier
    }

    fun setUsedProxies(supplier: () -> List<String>) {
        suppliers[Category.USED_PROXIES] = supplier
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCategory(category: Category): T {
        return suppliers[category]?.invoke() as? T ?: throw RuntimeException("Missing supplier for setting $category")
    }

    class LocalSettings(var repositories: List<Repository> = emptyList(),
                        var repositorySelection: Repository? = null,
                        var tree: List<TreeItem> = emptyList(),
                        var window: WindowSettings = WindowSettings(),
                        var tabSelection: Int = 0,
                        var usedProxies: List<String> = emptyList())

    class TreeItem(var value: String = "",
                   var expanded: Boolean = false)

    class WindowSettings(var x: Double = 0.0,
                         var y: Double = 0.0,
                         var width: Double = 0.0,
                         var height: Double = 0.0,
                         var maximized: Boolean = false,
                         var fullscreen: Boolean = false)

    enum class Category { REPOSITORIES, REPO_SELECTION, TREE, WINDOW, TAB_SELECTION, USED_PROXIES }

}
