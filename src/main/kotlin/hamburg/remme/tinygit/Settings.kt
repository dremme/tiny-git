package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import java.io.File
import java.nio.file.Files

object Settings {

    private val yaml = Yaml(Representer().also { it.propertyUtils.setSkipMissingProperties(true) })
    private val settingsFile = File("${System.getProperty("user.home")}/.tinygit").toPath()
    private val suppliers: MutableMap<Category, () -> Any> = mutableMapOf()
    private var settings: LocalSettings? = null

    fun load(block: LocalSettings.() -> Unit) {
        if (Files.exists(settingsFile)) {
            try {
                settings = yaml.loadAs(Files.newInputStream(settingsFile), LocalSettings::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        settings?.let(block)
    }

    fun save() {
        Files.write(settingsFile, yaml.dump(LocalSettings(
                getCategory(Category.REPOSITORY),
                getCategory(Category.TREE),
                getCategory(Category.TREE_SELECTION)))
                .toByteArray())
    }

    fun setRepository(supplier: () -> List<LocalRepository>) {
        suppliers[Category.REPOSITORY] = supplier
    }

    fun setTree(supplier: () -> List<TreeItem>) {
        suppliers[Category.TREE] = supplier
    }

    fun setTreeSelection(supplier: () -> TreeItem) {
        suppliers[Category.TREE_SELECTION] = supplier
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCategory(category: Category): T {
        return suppliers[category]?.invoke() as? T ?: throw RuntimeException("Missing supplier for setting $category")
    }

    class LocalSettings(var repositories: List<LocalRepository> = emptyList(),
                        var tree: List<TreeItem> = emptyList(),
                        var treeSelection: TreeItem = TreeItem())

    class TreeItem(var repository: String = "",
                   var name: String = "",
                   var expanded: Boolean = false)

    enum class Category { REPOSITORY, TREE, TREE_SELECTION }

}
