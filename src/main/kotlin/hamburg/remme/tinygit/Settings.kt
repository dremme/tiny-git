package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files

object Settings {

    private val yaml = Yaml()
    private val settingsFile = File("${System.getProperty("user.home")}/.tinygit").toPath()
    private val suppliers: MutableMap<Category, () -> Any> = mutableMapOf()
    private var settings: LocalSettings? = null

    fun load(block: (LocalSettings) -> Unit) {
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
                getCategory(Category.TREE_NODE_SELECTED)))
                .toByteArray())
    }

    fun setRepository(supplier: () -> List<LocalRepository>) {
        suppliers[Category.REPOSITORY] = supplier
    }

    fun setTree(supplier: () -> List<TreeNode>) {
        suppliers[Category.TREE] = supplier
    }

    fun setTreeNodeSelected(supplier: () -> TreeNode) {
        suppliers[Category.TREE_NODE_SELECTED] = supplier
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCategory(category: Category): T {
        return suppliers[category]?.invoke() as? T ?: throw RuntimeException("Missing supplier for setting $category")
    }

    class LocalSettings(var repositories: List<LocalRepository> = listOf(),
                        var tree: List<TreeNode> = listOf(),
                        var treeNodeSelected: TreeNode = TreeNode())

    class TreeNode(var repository: String = "",
                   var name: String = "",
                   var expanded: Boolean = false)

    enum class Category { REPOSITORY, TREE, TREE_NODE_SELECTED }

}
