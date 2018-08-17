package hamburg.remme.tinygit

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A delegate preventing a property being set more than once.
 */
internal class LateImmutable<T>(private var value: T? = null) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (value == null) throw IllegalStateException("Value is not initialized.")
        return value!!
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        if (this.value != null) throw IllegalStateException("Value is already initialized")
        this.value = value
    }

}
