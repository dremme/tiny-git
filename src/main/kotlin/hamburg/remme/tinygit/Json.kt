package hamburg.remme.tinygit

class Json(backingMap: Map<String, *> = emptyMap<String, Any>()) : HashMap<String, Any>(backingMap) {

    @Suppress("UNCHECKED_CAST")
    override fun get(property: String) = (super.get(property) as? Map<String, *>)?.let { Json(it) }

    @Suppress("UNCHECKED_CAST")
    fun getList(property: String) = (super.get(property) as? List<*>)?.map { Json(it as Map<String, *>) }

    fun getString(property: String) = super.get(property) as? String

    fun getStringList(property: String) = (super.get(property) as? List<*>)?.map { it as String }

    fun getBoolean(property: String) = super.get(property) as? Boolean

    fun getBooleanList(property: String) = (super.get(property) as? List<*>)?.map { it as Boolean }

    fun getInt(property: String) = super.get(property) as? Int

    fun getIntList(property: String) = (super.get(property) as? List<*>)?.map { it as Int }

    fun getDouble(property: String) = super.get(property) as? Double

    fun getDoubleList(property: String) = (super.get(property) as? List<*>)?.map { it as Double }

    operator fun Pair<String, Any>.unaryPlus() {
        put(first, second)
    }

    operator fun String.unaryMinus() {
        remove(this)
    }

}

inline fun json(block: Json.() -> Unit): Json {
    val json = Json()
    block.invoke(json)
    return json
}
