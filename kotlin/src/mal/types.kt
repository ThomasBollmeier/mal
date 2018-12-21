package mal

abstract class MalType  {

    open fun eval(env: Env) = this

}

abstract class MalSequence(val elements: List<MalType>) : MalType()

class MalList(elements: List<MalType>) : MalSequence(elements) {

    override fun eval(env: Env): MalType {

        if (isEmpty()) return this

        val first = elements[0]

        return when {
            isSpecialForm(first, "def!") -> evalDefinition(env)
            isSpecialForm(first, "let*") -> evalLet(env)
            else -> {
                val firstEval = elements[0].eval(env)
                when (firstEval) {
                    is MalFunction -> evalFunction(firstEval, env)
                    else -> MalError("$first not found")
                }
            }
        }

    }

    private fun isEmpty() = elements.isEmpty()

    private fun isSpecialForm(value: MalType, name: String) =
            value is MalSymbol && value.toString() == name

    private fun evalFunction(fn: MalFunction, env: Env) : MalType {
        val evaluated = elements.map { it.eval(env)}
        val numArgs = evaluated.size - 1
        val args = (if (numArgs > 0)
            evaluated.subList(1, numArgs + 1)
        else
            emptyList()).toTypedArray()
        return fn.apply(args)
    }

    private fun evalDefinition(env: Env) : MalType {
        return if (elements.size == 3) {
            val symbol = elements[1]
            if (symbol is MalSymbol) {
                val value = elements[2].eval(env)
                if (value !is MalError) env[symbol.toString()] = value
                value
            } else MalError("First argument in definition must be a symbol")
        } else MalError("Too many arguments in definition")
    }

    private fun evalLet(env: Env) : MalType {
        return if (elements.size == 3) {
            val bindings = elements[1]
            if (bindings is MalSequence) {
                val letEnv = Env(env)
                try {
                    setBindings(letEnv, bindings)
                    elements[2].eval(letEnv)
                } catch (error: BindingsError) {
                    MalError(error.toString())
                }
            } else MalError("First argument in let expression must be a list")
        } else MalError("Too many arguments in let expression")
    }

    class BindingsError : Exception()

    private fun setBindings(env: Env, bindings: MalSequence) {

        var symbol = ""

        for ((i, elem) in bindings.elements.withIndex()) {
            if (i % 2 == 0) {
                if (elem is MalSymbol) {
                    symbol = elem.toString()
                } else throw BindingsError()
            } else {
                env[symbol] = elem.eval(env)
            }
        }
    }

    override fun toString() = elements.joinToString(
            separator = " ", prefix = "(", postfix = ")") { it.toString() }

}

class MalVector(elements: List<MalType>) : MalSequence(elements) {

    override fun eval(env: Env) = MalVector(elements.map { it.eval(env) })

    override fun toString() = elements.joinToString(
            separator = " ", prefix = "[", postfix = "]") { it.toString() }

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

    override fun eval(env: Env): MalType = try {
            env[value]
        } catch (error: ValueNotFound) {
            MalError(error.toString())
        }

    override fun toString() = value

}

class MalString(private val value: String) : MalType() {

    override fun toString() = value

}

class MalFunction(private val callable: (Array<MalType>) -> MalType) : MalType() {

    override fun toString() = "#<function>"

    fun apply(args: Array<MalType> = emptyArray()) = callable(args)

}

class MalError(private val errorMsg: String) : MalType() {

    override fun toString() = errorMsg

}