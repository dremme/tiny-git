package hamburg.remme.tinygit

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import kotlin.reflect.KClass

/**
 * Finds all [KClass]es annotated with the given [Annotation].
 */
inline fun <reified T : Annotation> findAll(): List<KClass<*>> {
    val classes = mutableListOf<KClass<*>>()
    FastClasspathScanner(TinyGit::class.java.packageName)
            .matchClassesWithAnnotation(T::class.java, { classes += it.kotlin })
            .scan(Runtime.getRuntime().availableProcessors())
    return classes
}

/**
 * Creates a [Map] containing values that are instances of their respective [KClass] key.
 * Any dependencies between the classes will be resolved automatically.
 * They are instantiated in order of their dependency tree.
 */
fun List<KClass<*>>.createSingletonMap(): Map<KClass<*>, Any> {
    val dependencyMap = mutableMapOf<KClass<*>, Any>()
    sortedByDependencies().forEach {
        val constructor = it.java.constructors[0]
        val arguments = constructor.parameters.map { dependencyMap[it.type.kotlin] }
        dependencyMap += it to constructor.newInstance(*arguments.toTypedArray())
    }
    return dependencyMap
}

/**
 * Sorts the list in order of the classes dependency tree.
 */
private fun List<KClass<*>>.sortedByDependencies() = sortedWith(Comparator { s1, s2 ->
    val param1 = s1.java.constructors[0].parameters
    val param2 = s2.java.constructors[0].parameters
    when {
    // Both constructors have 0 parameters
        param1.isEmpty() && param2.isEmpty() -> 0
    // One has 0 parameters
        param1.isEmpty() && param2.isNotEmpty() -> -1
        param1.isNotEmpty() && param2.isEmpty() -> 1
    // Both classes depend on each other -> cyclic dependency
        param1.any { it.type.kotlin == s2 } && param2.any { it.type.kotlin == s1 }
        -> throw RuntimeException("Cyclic dependency between $s1 and $s2 detected.")
    // One class has the other one as dependency
        param2.any { it.type.kotlin == s1 } -> -1
        param1.any { it.type.kotlin == s2 } -> 1
    // No dependencies between the two
        else -> 0
    }
})
