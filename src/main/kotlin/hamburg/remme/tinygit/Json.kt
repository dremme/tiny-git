package hamburg.remme.tinygit

class Json(backingMap: Map<String, *> = emptyMap<String, Any>()) : HashMap<String, Any>(backingMap) {

    @Suppress("UNCHECKED_CAST")
    fun getObject(property: String) = (this[property] as? Map<String, *>)?.let { Json(it) }

    @Suppress("UNCHECKED_CAST")
    fun getObjectList(property: String) = (this[property] as? List<*>)?.map { Json(it as Map<String, *>) }

    fun getString(property: String) = this[property] as? String

    fun getStringList(property: String) = (this[property] as? List<*>)?.map { it as String }

    fun getBoolean(property: String) = this[property] as? Boolean

    fun getBooleanList(property: String) = (this[property] as? List<*>)?.map { it as Boolean }

    fun getInt(property: String) = this[property] as? Int

    fun getIntList(property: String) = (this[property] as? List<*>)?.map { it as Int }

    fun getDouble(property: String) = this[property] as? Double

    fun getDoubleList(property: String) = (this[property] as? List<*>)?.map { it as Double }

    operator fun Pair<String, Any>.unaryPlus() {
        this@Json[first] = second
    }

}

inline fun json(block: Json.() -> Unit): Json {
    val json = Json()
    block.invoke(json)
    return json
}
