package hamburg.remme.tinygit.git

class LocalGraph(commits: List<LocalCommit>) : Iterable<LocalGraph.Node> {

    private val nodes = mutableListOf<Node>()

    init {
        commits.forEach {
            val child = nodes.addIfAbsent(it.id)
            it.parents.forEach {
                val parent = nodes.addIfAbsent(it)
                parent.children.add(child)
                child.parents.add(parent)
            }
        }
        nodes.sortBy { node -> commits.indexOfFirst { it.id == node.commit }.takeIf { it >= 0 } ?: Integer.MAX_VALUE }
    }

    private fun MutableList<Node>.addIfAbsent(commit: String): Node {
        return find { it.commit == commit } ?: Node(commit).also { add(it) }
    }

    override fun iterator() = nodes.iterator()

    operator fun get(index: Int) = nodes[index]

    class Node(val commit: String) {

        val children = mutableListOf<Node>()
        val parents = mutableListOf<Node>()

    }

}
