package mal

open class MalType

class MalList(val elements: List<MalType>) : MalType() {

    fun isEmpty() = elements.isEmpty()

    override fun toString() = elements.map { it.toString() }.joinToString(
                separator = " ", prefix = "(", postfix = ")")

}

class MalVector(private val elements: List<MalType>) : MalType() {

    override fun toString() = elements.map { it.toString() }.joinToString(
            separator = " ", prefix = "[", postfix = "]")

}

class MalHashMap(private val keys: List<MalType>, private val values: List<MalType>) : MalType() {

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

class MalNil() : MalType() {

    override fun toString() = "nil"

}

class MalSymbol(val value: String) : MalType() {

    override fun toString() = value

}

class MalString(private val value: String) : MalType() {

    override fun toString() = value

}

class MalFunction(private val callable: (Array<MalType>) -> MalType) : MalType() {

    override fun toString() = "<function>"

    fun call(args: Array<MalType> = emptyArray()) = callable(args)

}

class MalError(private val errorMsg: String) : MalType() {

    override fun toString() = errorMsg

}