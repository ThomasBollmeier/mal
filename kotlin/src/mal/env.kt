package mal

class ValueNotFound(message: String) : Exception(message)

class Env(private val outer: Env? = null) {

    private val data = mutableMapOf<String, MalType>()

    operator fun set(key: String, value: MalType) {
        data[key] = value
    }

    operator fun get(key: String) =
            find(key)?.data?.get(key) ?:
                    throw ValueNotFound("Symbol $key is unknown")

    fun find(key: String) : Env? =
            when {
                data.containsKey(key) -> this
                outer != null -> outer.find(key)
                else -> null
            }

}