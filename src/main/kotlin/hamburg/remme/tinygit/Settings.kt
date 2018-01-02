package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer

object Settings {

    private val yaml = Yaml(Representer().apply { propertyUtils.setSkipMissingProperties(true) },
            DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK })
    private val settingsFile = "${System.getProperty("user.home")}/.tinygit".asPath()
    private val suppliers: MutableMap<Category, () -> Any> = mutableMapOf()
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
                getCategory(Category.TREE),
                getCategory(Category.TREE_SELECTION),
                getCategory(Category.WINDOW),
                getCategory(Category.TAB_SELECTION))))
    }

    fun setRepositories(supplier: () -> List<LocalRepository>) {
        suppliers[Category.REPOSITORIES] = supplier
    }

    fun setTree(supplier: () -> List<TreeItem>) {
        suppliers[Category.TREE] = supplier
    }

    fun setTreeSelection(supplier: () -> TreeItem) {
        suppliers[Category.TREE_SELECTION] = supplier
    }

    fun setWindow(supplier: () -> WindowSettings) {
        suppliers[Category.WINDOW] = supplier
    }

    fun setTabSelection(supplier: () -> Int) {
        suppliers[Category.TAB_SELECTION] = supplier
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCategory(category: Category): T {
        return suppliers[category]?.invoke() as? T ?: throw RuntimeException("Missing supplier for setting $category")
    }

    class LocalSettings(var repositories: List<LocalRepository> = emptyList(),
                        var tree: List<TreeItem> = emptyList(),
                        var treeSelection: TreeItem = TreeItem(),
                        var window: WindowSettings = WindowSettings(),
                        var tabSelection: Int = 0)

    class TreeItem(var repository: String = "",
                   var name: String = "",
                   var expanded: Boolean = false)

    class WindowSettings(var x: Double = 0.0,
                         var y: Double = 0.0,
                         var width: Double = 0.0,
                         var height: Double = 0.0,
                         var maximized: Boolean = false,
                         var fullscreen: Boolean = false)

    enum class Category { REPOSITORIES, TREE, TREE_SELECTION, WINDOW, TAB_SELECTION }

}
