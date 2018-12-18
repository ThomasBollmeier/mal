package mal

abstract class MalType  {

    open fun eval(env: Env) = this

}

class MalList(private val elements: List<MalType>) : MalType() {

    fun isEmpty() = elements.isEmpty()

    override fun eval(env: Env): MalType {

        if (isEmpty()) return this

        val evaluated = elements.map { it.eval(env)}
        val fn = evaluated[0]
        val numArgs = evaluated.size - 1
        val args = (if (numArgs > 0)
            evaluated.subList(1, numArgs + 1)
        else
            emptyList()).toTypedArray()

        return if (fn is MalFunction)
            fn.apply(args)
        else MalError("function expected")

    }

    override fun toString() = elements.map { it.toString() }.joinToString(
                separator = " ", prefix = "(", postfix = ")")

}

class MalVector(private val elements: List<MalType>) : MalType() {

    override fun eval(env: Env) = MalVector(elements.map { it.eval(env) })

    override fun toString() = elements.map { it.toString() }.joinToString(
            separator = " ", prefix = "[", postfix = "]")

}

class MalHashMap(private val keys: List<MalType>, private val values: List<MalType>) : MalType() {

    override fun eval(env: Env) = MalHashMap(keys, values.map { it.eval(env)})

    override fun toString() : String {
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((i, key) in keys.withIndex()) {
            if (!first) sb.append(", ") else first = false
            sb.append(key.toString())
            sb.append(" ")
            sb.append(values[i].toString())
        }
        sb.append('}')
        return sb.toString()
    }

}

class MalNumber(val value: Int) : MalType() {

    override fun toString() = value.toString()

}

class MalBoolean(private val value: Boolean) : MalType() {

    override fun toString() = if (value) "true" else "false"

}

class MalNil : MalType() {

    override fun toString() = "nil"

}

class MalSymbol(private val value: String) : MalType() {

    override fun eval(env: Env): MalType {
        return env.getOrDefault(value, MalError("$value not found"))
    }

    override fun toString() = value

}

class MalString(private val value: String) : MalType() {

    override fun toString() = value

}

class MalFunction(private val callable: (Array<MalType>) -> MalType) : MalType() {

    override fun toString() = "<function>"

    fun apply(args: Array<MalType> = emptyArray()) = callable(args)

}

class MalError(private val errorMsg: String) : MalType() {

    override fun toString() = errorMsg

}