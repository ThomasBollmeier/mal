package mal

abstract class MalType  {

    open fun eval(env: Env) = this

    override operator fun equals(other: Any?) =
            if (other != null)
                javaClass == other.javaClass
            else
                false

    open fun toString(readably: Boolean) = toString()

}

abstract class MalSequence(val elements: List<MalType>) : MalType() {

    override fun equals(other: Any?) =
            other != null && other is MalSequence && elements == other.elements

    fun isEmpty() = elements.isEmpty()

}

class MalList(elements: List<MalType>) : MalSequence(elements) {

    override fun eval(env: Env): MalType {

        if (isEmpty()) return this

        val first = elements[0]

        return when {
            isSpecialForm(first, "def!") -> evalDefinition(env)
            isSpecialForm(first, "let*") -> evalLet(env)
            isSpecialForm(first, "do") -> evalDo(env)
            isSpecialForm(first, "if") -> evalIf(env)
            isSpecialForm(first, "fn*") -> evalLambda(env)
            else -> {
                val firstEval = elements[0].eval(env)
                when (firstEval) {
                    is MalFunction -> evalFunction(firstEval, env)
                    else -> MalError("$first not found")
                }
            }
        }

    }

    private fun isSpecialForm(value: MalType, name: String) =
            value is MalSymbol && value.toString() == name

    private fun evalFunction(fn: MalFunction, env: Env) : MalType {
        val evaluated = elements.map { it.eval(env)}
        val numArgs = evaluated.size - 1
        val args = if (numArgs > 0)
            evaluated.subList(1, numArgs + 1)
        else
            emptyList()
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

    private fun evalDo(env: Env) : MalType {
        var result: MalType = MalNil()
        for (i in 1 until elements.size) {
            result = elements[i].eval(env)
        }
        return result
    }

    private fun evalIf(env: Env) : MalType {

        if (elements.size < 3 || elements.size > 4)
            return MalError("invalid if expression")

        val condValue = elements[1].eval(env)
        val condition = when (condValue) {
            is MalBoolean -> condValue.value
            is MalNil -> false
            else -> true
        }

        return when {
            condition -> elements[2].eval(env)
            elements.size == 4 -> elements[3].eval(env)
            else -> MalNil()
        }

    }

    private fun evalLambda(env: Env) : MalType {

        if (elements.size != 3)
            return MalError("invalid lambda expression")

        val params = elements[1] as? MalSequence ?:
            return MalError("invalid parameter format")

        if (!params.elements.all { it is MalSymbol })
            return MalError("invalid parameter format")

        val paramNames = mutableListOf<String>()
        var varargParam : String? = null

        var varargPending = false
        for (param in params.elements) {
            val paramName = param.toString()
            if (!varargPending) {
                if (paramName != "&")
                    paramNames += paramName
                else
                    varargPending = true
            } else {
                varargParam = paramName
                break
            }
        }

        val body = elements[2]

        return MalFunction { args: List<MalType> ->

            if (paramNames.size == args.size) {
                if (varargParam == null)
                    body.eval(Env(env, paramNames, args))
                else {
                    val bindings = mutableListOf<String>()
                    bindings.addAll(paramNames)
                    bindings.add(varargParam)
                    val expressions =  mutableListOf<MalType>()
                    expressions.addAll(args)
                    expressions.add(MalList(emptyList()))
                    body.eval(Env(env, bindings, expressions))
                }
            } else if (paramNames.size < args.size)
                if (varargParam != null) {
                    val bindings = mutableListOf<String>()
                    bindings.addAll(paramNames)
                    bindings.add(varargParam)
                    val expressions = mutableListOf<MalType>()
                    expressions.addAll(args.subList(0, paramNames.size))
                    val varargExprs = args.subList(paramNames.size, args.size)
                    expressions.add(MalList(varargExprs))
                    body.eval(Env(env, bindings, expressions))
                } else
                    MalError("too many arguments given")
            else
                MalError("too few arguments given")
        }
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

    override fun toString() = toString(false)

    override fun toString(readably: Boolean) = elements.joinToString(
            separator = " ", prefix = "(", postfix = ")") { it.toString(readably) }

}

class MalVector(elements: List<MalType>) : MalSequence(elements) {

    override fun eval(env: Env) = MalVector(elements.map { it.eval(env) })

    override fun toString() = toString(false)

    override fun toString(readably: Boolean) = elements.joinToString(
            separator = " ", prefix = "[", postfix = "]") { it.toString(readably) }

}

class MalHashMap(private val keys: List<MalType>, private val values: List<MalType>) : MalType() {

    override fun eval(env: Env) = MalHashMap(keys, values.map { it.eval(env)})

    override fun toString() = toString(false)

    override fun toString(readably: Boolean) : String {
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((i, key) in keys.withIndex()) {
            if (!first) sb.append(", ") else first = false
            sb.append(key.toString(readably))
            sb.append(" ")
            sb.append(values[i].toString(readably))
        }
        sb.append('}')
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                (other as? MalHashMap)?.keys?.equals(keys) ?: false &&
                (other as? MalHashMap)?.values?.equals(values) ?: false
    }

}

class MalNumber(val value: Int) : MalType() {

    override fun toString() = value.toString()

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                (other as? MalNumber)?.value?.equals(value) ?: false
    }

}

class MalBoolean(val value: Boolean) : MalType() {

    override fun toString() = if (value) "true" else "false"

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                (other as? MalBoolean)?.value?.equals(value) ?: false
    }

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

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                (other as? MalSymbol)?.value?.equals(value) ?: false
    }

}

class MalKeyword(private val value: String) : MalType() {

    override fun toString() = value

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                (other as? MalKeyword)?.value?.equals(value) ?: false
    }

}

class MalString(val value: String) : MalType() {

    companion object {

        fun unquote(s: String) : String {

            if (s.length <= 2) return ""

            val sb = StringBuilder()
            var previous : Char? = null
            val chars = s.substring(1..(s.length-2))

            loop@ for (ch in chars) {
                if (previous != '\\') {
                    if (ch != '\\')
                        sb.append(ch)
                } else {
                    when (ch) {
                        'n' -> sb.append('\n')
                        '\"' -> sb.append(ch)
                        '\\' -> {
                            sb.append(ch)
                            previous = null
                            continue@loop
                        }
                        else -> {
                            sb.append(previous)
                            sb.append(ch)
                        }
                    }
                }

                previous = ch
            }

            return sb.toString()
        }

        fun quote(s: String) : String {

            var res = s
            res = res.replace("\\", "\\\\")
            res = res.replace("\n", "\\n")
            res = res.replace("\"", "\\\"")
            res = "\"$res\""
            return res
        }

    }

    override fun toString() = toString(false)

    override fun toString(readably: Boolean) =
            if (readably)
                MalString.quote(value)
            else
                value

    override fun equals(other: Any?): Boolean {
        return super.equals(other) &&
                (other as? MalString)?.value?.equals(value) ?: false
    }

}

class MalFunction(private val callable: (List<MalType>) -> MalType) : MalType() {

    override fun toString() = "#<function>"

    fun apply(args: List<MalType>) = callable(args)

}

class MalError(private val errorMsg: String) : MalType() {

    override fun toString() = errorMsg

}