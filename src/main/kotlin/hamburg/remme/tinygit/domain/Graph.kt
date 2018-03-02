package hamburg.remme.tinygit.domain

class Graph : ArrayList<Graph.GraphNode>() {

    companion object {
        fun of(commits: List<Commit>): Graph {
            val graph = Graph()
            commits.forEach { trace(it, graph) }
            graph.sortWith(compareBy { node -> commits.indexOfFirst { it.id == node.id }.takeIf { it >= 0 } ?: Int.MAX_VALUE })
            graph.fold(0, { acc, it -> tag(it, acc) })
            return graph
        }

        private fun trace(commit: Commit, graph: Graph) {
            val node = graph.getOrAdd(commit.id)
            commit.parents.forEach {
                val parent = graph.getOrAdd(it)
                node.parents += parent
                parent.children += node
            }
        }

        private fun tag(node: GraphNode, tag: Int): Int {
            if (node.tag < 0) {
                var newTag = tag
                node.tag = newTag
                node.parents.forEachIndexed { i, it ->
                    if (i > 0) newTag++
                    newTag = tag(it, newTag)
                }
                return newTag
            }
            return tag
        }
    }

    fun getOrAdd(id: String) = find { it.id == id } ?: GraphNode(id).also { add(it) }

    class GraphNode(val id: String) {

        val children = LinkedHashSet<GraphNode>()
        val parents = LinkedHashSet<GraphNode>()
        val isHead get() = children.isEmpty()
        val isTail get() = parents.isEmpty()
        var tag = -1

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GraphNode

            if (id != other.id) return false

            return true
        }

        override fun hashCode() = id.hashCode()

    }

}
