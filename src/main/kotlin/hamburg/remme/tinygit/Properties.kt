package hamburg.remme.tinygit

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate preventing a property being set more than once.
 */
internal fun <T> lateVal(): LateVal<T> = LateVal()

internal class LateVal<T> : ReadWriteProperty<Any, T> {

    private var value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (value == null) throw IllegalStateException("Value is not initialized.")
        return value!!
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        if (this.value != null) throw IllegalStateException("Value is already initialized")
        this.value = value
    }

}

/**
 * Like [lazy], but as variable.
 */
internal fun <T> lazyVar(initializer: () -> T): LazyVar<T> = LazyVar(initializer)

internal class LazyVar<T>(private val initializer: () -> T) : ReadWriteProperty<Any, T> {

    private var value: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (value == null) value = initializer()
        return value!!
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
    }

}
