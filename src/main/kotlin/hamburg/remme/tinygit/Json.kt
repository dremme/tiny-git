package hamburg.remme.tinygit

class Json(private val backingMap: LinkedHashMap<String, Any> = LinkedHashMap()) {

    @Suppress("UNCHECKED_CAST")
    operator fun get(property: String) = (backingMap[property] as? LinkedHashMap<String, Any>)?.let { Json(it) }

    operator fun set(property: String, value: Any) = backingMap.put(property, value)

    @Suppress("UNCHECKED_CAST")
    fun getList(property: String) = (backingMap[property] as? List<*>)?.map { Json(it as LinkedHashMap<String, Any>) }

    fun getString(property: String) = backingMap[property] as? String

    fun getStringList(property: String) = (backingMap[property] as? List<*>)?.map { it as String }

    fun getBoolean(property: String) = backingMap[property] as? Boolean

    fun getBooleanList(property: String) = (backingMap[property] as? List<*>)?.map { it as Boolean }

    fun getInt(property: String) = backingMap[property] as? Int

    fun getIntList(property: String) = (backingMap[property] as? List<*>)?.map { it as Int }

    fun getDouble(property: String) = backingMap[property] as? Double

    fun getDoubleList(property: String) = (backingMap[property] as? List<*>)?.map { it as Double }

    operator fun Pair<String, Any>.unaryPlus() = backingMap.put(first, second)

    operator fun String.unaryMinus() = backingMap.remove(this)

}

inline fun json(block: Json.() -> Unit): Json {
    val json = Json()
    block.invoke(json)
    return json
}
