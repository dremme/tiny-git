package hamburg.remme.tinygit

import java.lang.reflect.Parameter
import java.util.LinkedList
import kotlin.reflect.KClass

const val BASE_PACKAGE = "hamburg.remme.tinygit"
const val BASE_PATH = "hamburg/remme/tinygit"
const val CLASS_EXTENSION = ".class"
/**
 * The default [ClassLoader] retrieved from [Thread.currentThread].
 */
val classLoader = Thread.currentThread().contextClassLoader!!
/**
 * The primary constructor of the [KClass] based on the Kotlin language.
 */
val KClass<*>.primaryConstructor get() = java.constructors[0]!!
/**
 * The [Parameter]s of the primary constructor.
 * @see KClass.primaryConstructor
 */
val KClass<*>.primaryParameters: List<Parameter> get() = primaryConstructor.parameters.toList()

/**
 * **Warning: don't use this if you don't know what you are doing!**
 *
 * Scans the class-path for [Class]es and returns them as [Sequence].
 */
fun scanClassPath(): Sequence<Class<*>> {
    return (if (isJar()) jarClasses() else fileClasses())
            .map { it.toString().substringAfter("$BASE_PATH/").replace('/', '.') }
            .map { "$BASE_PACKAGE.$it" }
            .map { it.substring(0, it.length - CLASS_EXTENSION.length) }
            .map { Class.forName(it, false, classLoader) }
}

/**
 * **Warning: don't use this if you don't know what you are doing!**
 */
private fun jarClasses() = jarFile().entries().asSequence()
        .map { it.name }
        .filter { it.startsWith(BASE_PATH) }
        .filter { it.endsWith(CLASS_EXTENSION) }

/**
 * **Warning: don't use this if you don't know what you are doing!**
 */
private fun fileClasses() = classLoader.getResources(BASE_PATH).asSequence()
        .flatMap { it.file.asPath().walk() }
        .filter { it.extensionEquals(CLASS_EXTENSION) }

/**
 * **Warning: don't use this if you don't know what you are doing!**
 *
 * Finds all [KClass]es in the class-path annotated with the [Annotation].
 */
inline fun <reified T : Annotation> scanAnnotation() = scanClassPath()
        .filter { it.isAnnotationPresent(T::class.java) }
        .map { it.kotlin }
        .toList()

/**
 * **Warning: don't use this if you don't know what you are doing!**
 *
 * Creates a [Map] containing values that are instances of their respective [KClass] key.
 * Any dependencies between the classes will be resolved automatically.
 * They are instantiated in order of their dependencies using a topological sorting.
 */
fun createDependencyMap(classes: List<KClass<*>>): Map<KClass<*>, Any> {
    val dependencyMap = mutableMapOf<KClass<*>, Any>()
    classes.sortedByDependencies().forEach {
        val arguments = it.primaryParameters.map { dependencyMap[it.type.kotlin] }
        dependencyMap += it to it.primaryConstructor.newInstance(*arguments.toTypedArray())
    }
    return dependencyMap
}

/**
 * Sorts the list in order of the classes dependency tree.
 */
private fun List<KClass<*>>.sortedByDependencies(): List<KClass<*>> {
    val graph = mutableSetOf<ClassNode>()
    forEach { graph.getOrCreate(it).neighbors += it.primaryParameters.map { graph.getOrCreate(it.type.kotlin) } }
    return topologicalSort(graph).map { it.value }
}

private fun MutableSet<ClassNode>.getOrCreate(klass: KClass<*>) = find { it.value == klass } ?: ClassNode(klass).also { this += it }

/**
 * Implemented as topological sort like here:
 * https://en.wikipedia.org/wiki/Topological_sorting
 *
 * @todo: maybe use depth-first sorting instead
 */
private fun topologicalSort(graph: Set<ClassNode>): List<ClassNode> {
    val indegree = mutableMapOf<ClassNode, Int>()
    graph.forEach { indegree[it] = 0 }
    graph.flatMap { it.neighbors }.forEach { indegree[it] = indegree[it]!! + 1 }

    val sorted = mutableListOf<ClassNode>()
    val queue = LinkedList<ClassNode>()

    graph.filter { indegree[it] == 0 }.forEach {
        queue.offer(it)
        sorted.add(0, it)
    }

    while (queue.isNotEmpty()) {
        queue.poll().neighbors.forEach {
            indegree[it] = indegree[it]!! - 1
            if (indegree[it] == 0) {
                queue.offer(it)
                sorted.add(0, it)
            }
        }
    }

    if (sorted.size != graph.size) throw RuntimeException("Cyclic dependencies detected.")
    return sorted
}

/**
 * A directed graph node used in [topologicalSort].
 */
private class ClassNode(val value: KClass<*>, val neighbors: MutableList<ClassNode> = mutableListOf())
