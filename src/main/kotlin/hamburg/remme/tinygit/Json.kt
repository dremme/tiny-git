package hamburg.remme.tinygit

class Json(backingMap: Map<String, *>) : HashMap<String, Any>(backingMap) {

    @Suppress("UNCHECKED_CAST")
    fun getObject(property: String) = Json(this[property] as Map<String, *>)

    @Suppress("UNCHECKED_CAST")
    fun getList(property: String) = (this[property] as List<*>).forEach { Json(it as Map<String, *>) }

    fun getString(property: String) = this[property] as String

    fun getBoolean(property: String) = this[property] as Boolean

    fun getInt(property: String) = this[property] as Int

    fun getDouble(property: String) = this[property] as Double

    operator fun Pair<String, Any>.unaryPlus() {
        this@Json[first] = second
    }

}

inline fun json(block: Json.() -> Unit): Json {
    val json = Json(emptyMap<String, Any>())
    block.invoke(json)
    return json
}
