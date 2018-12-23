package mal

class ValueNotFound(message: String) : Exception(message)
class BindingError(message:String) : Exception(message)

class Env(private val outer: Env? = null,
          binds: List<String> = emptyList(),
          exprs: List<MalType> = emptyList()) {

    private val data = mutableMapOf<String, MalType>()

    init {
        if (binds.size != exprs.size)
            throw BindingError("#arguments does not match #parameters")
        for ((i, param) in binds.withIndex()) {
            data[param] = exprs[i]
        }
    }

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