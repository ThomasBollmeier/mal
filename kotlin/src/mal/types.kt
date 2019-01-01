package mal

class EvalResult( val result: MalType?,
                  val type: MalType? = null,
                  val env: Env? = null)

class MalException(val type: MalType) : Exception()

abstract class MalType  {

    fun eval(env: Env) : MalType {

        var type = this
        var currEnv = env
        
        while (true) {
            val evalResult = type.evalInternal(currEnv)
            if (evalResult.result != null) {
                return evalResult.result
            }
            type = evalResult.type!!
            currEnv = evalResult.env!!
        }
    }

    protected open fun evalInternal(env: Env) : EvalResult = this.toResult()

    fun toResult() = EvalResult(this)

    override operator fun equals(other: Any?) =
            if (other != null)
                javaClass == other.javaClass
            else
                false

    open fun toString(readably: Boolean) = toString()

}

abstract class MalSequence(public val elements: List<MalType>) : MalType() {

    companion object {

        fun concat(vararg lists: MalSequence) : MalList {
            val allElements = mutableListOf<MalType>()
            for (list in lists) {
                allElements.addAll(list.elements)
            }
            return MalList(allElements)
        }

    }

    override fun equals(other: Any?) =
            other != null && other is MalSequence && elements == other.elements

    fun isEmpty() = elements.isEmpty()

    fun cons(head: MalType) : MalList {
        val newElements = mutableListOf<MalType>()
        newElements.add(head)
        newElements.addAll(elements)
        return MalList(newElements)
    }

    fun head() = if (elements.isNotEmpty())
        elements.first()
    else
        MalNil()

    fun tail() = if (elements.isNotEmpty())
        MalList(elements.drop(1))
    else
        MalList(emptyList())

    fun nth(n: Int) = elements.getOrNull(n) ?: MalError("Index out of bounds")

}

class MalList(elements: List<MalType>) : MalSequence(elements) {

    override fun evalInternal(env: Env): EvalResult {

        if (isEmpty()) return this.toResult()

        val first = elements[0]
        if (isMacro(first, env)) {
            return EvalResult(null,
                    evalMacro(first as MalSymbol, env),
                    env)
        }

        return when {
            isSpecialForm(first, "def!") -> evalDefinition(env)
            isSpecialForm(first, "defmacro!") -> evalDefinition(env, true)
            isSpecialForm(first, "let*") -> evalLet(env)
            isSpecialForm(first, "do") -> evalDo(env)
            isSpecialForm(first, "if") -> evalIf(env)
            isSpecialForm(first, "fn*") -> evalLambda(env)
            isSpecialForm(first, "quote") -> elements[1].toResult()
            isSpecialForm(first, "quasiquote") -> evalQuasiQuote(env)
            isSpecialForm(first, "macroexpand") -> expandMacro(env)
            isSpecialForm(first, "try*") -> evalTryCatch(env)
            else -> {
                val firstEval = elements[0].eval(env)
                when (firstEval) {
                    is MalFunction -> evalFunction(firstEval, env)
                    else -> MalError("'$first' not found").toResult()
                }
            }
        }

    }

    private fun isSpecialForm(value: MalType, name: String) =
            value is MalSymbol && value.toString() == name

    private fun isMacroCall(value: MalType, env: Env) =
        value is MalList && !value.isEmpty() && isMacro(value.head(), env)

    private fun isMacro(value: MalType, env: Env) : Boolean {
        return if (value is MalSymbol) {
            val evaluated = value.eval(env)
            evaluated is MalFunction && evaluated.isMacro
        } else
            false
    }

    private fun evalTryCatch(env: Env) : EvalResult {
        // (try* A (catch* B C))
        if (elements.size != 3)
            return MalError("Illegal number of args").toResult()

        val tryExpr = elements[1]

        val catchBlock = elements[2] as? MalList ?:
            return MalError("catch block expected").toResult()
        val catch = catchBlock.head()
        if (catch !is MalSymbol && catch.toString() != "catch*")
            return MalError("catch block expected").toResult()
        val exception = catchBlock.tail().head() as? MalSymbol ?:
            return MalError("exception must be symbol").toResult()
        val catchExpr = catchBlock.tail().tail().head()

        return try {
            val res = tryExpr.eval(env).toResult()
            if (res.result !is MalError) {
                res
            } else {
                throw MalException(res.result)
            }
        } catch (exc: MalException) {
            val error = MalError.fromException(exc)
            val catchEnv = Env(env,
                    listOf(exception.toString()),
                    listOf(error))
            catchExpr.eval(catchEnv).toResult()
        }

    }

    private fun evalFunction(fn: MalFunction, env: Env) : EvalResult {
        val evaluated = elements.map { it.eval(env)}
        val numArgs = evaluated.size - 1
        val args = if (numArgs > 0)
            evaluated.subList(1, numArgs + 1)
        else
            emptyList()
        return fn.apply(args)
    }

    private fun evalMacro(macroSymbol: MalSymbol, env: Env) : MalType {
        val macro = macroSymbol.eval(env) as MalFunction
        val args = elements.drop(1)
        return macro.applyWithResult(args)
    }

    private fun expandMacro(env: Env) : EvalResult {

        var value = elements.drop(1).first()
        while (isMacroCall(value, env)) {
            val call = value as MalList
            val macro = call.head().eval(env) as MalFunction
            val args = call.elements.drop(1)
            value = macro.applyWithResult(args)
        }

        return value.toResult()
    }

    private fun evalDefinition(env: Env, isMacro: Boolean = false) : EvalResult {
        val type = if (elements.size == 3) {
            val symbol = elements[1]
            if (symbol is MalSymbol) {
                var value = elements[2].eval(env)
                if (isMacro) {
                    if (value is MalFunction)
                        value.isMacro = true
                    else
                        value = MalError("defmacro! requires function as value")
                }
                if (value !is MalError) env[symbol.toString()] = value
                value
            } else MalError("First argument in definition must be a symbol")
        } else MalError("Too many arguments in definition")
        return type.toResult()
    }

    private fun evalLet(env: Env) : EvalResult {
        return if (elements.size == 3) {
            val bindings = elements[1]
            if (bindings is MalSequence) {
                val letEnv = Env(env)
                try {
                    setBindings(letEnv, bindings)
                    EvalResult(null, elements[2], letEnv)
                } catch (error: BindingsError) {
                    MalError(error.toString()).toResult()
                }
            } else MalError("First argument in let expression must be a list")
                    .toResult()
        } else MalError("Too many arguments in let expression")
                .toResult()
    }

    private fun evalDo(env: Env) : EvalResult {

        if (elements.size > 2) {
            for (i in 1 until elements.size-1) {
                elements[i].eval(env)
            }
        }

        return EvalResult(null, elements[elements.size-1], env)
    }

    private fun evalIf(env: Env) : EvalResult {

        if (elements.size < 3 || elements.size > 4)
            return MalError("invalid if expression").toResult()

        val condValue = elements[1].eval(env)
        val condition = when (condValue) {
            is MalBoolean -> condValue.value
            is MalNil -> false
            else -> true
        }

        return when {
            condition -> EvalResult(null, elements[2], env)
            elements.size == 4 -> EvalResult(null, elements[3], env)
            else -> MalNil().toResult()
        }

    }

    private fun evalLambda(env: Env) : EvalResult {

        if (elements.size != 3)
            return MalError("invalid lambda expression").toResult()

        val params = elements[1] as? MalSequence ?:
            return MalError("invalid parameter format").toResult()

        if (!params.elements.all { it is MalSymbol })
            return MalError("invalid parameter format").toResult()

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
                    EvalResult(null, body, Env(env, paramNames, args))
                else {
                    val bindings = mutableListOf<String>()
                    bindings.addAll(paramNames)
                    bindings.add(varargParam)
                    val expressions =  mutableListOf<MalType>()
                    expressions.addAll(args)
                    expressions.add(MalList(emptyList()))
                    EvalResult(null, body, Env(env, bindings, expressions))
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
                    EvalResult(null, body, Env(env, bindings, expressions))
                } else
                    MalError("too many arguments given").toResult()
            else
                MalError("too few arguments given").toResult()
        }.toResult()

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

    private fun evalQuasiQuote(env: Env) : EvalResult {

        val arg = elements[1]
        if (arg !is MalSequence || arg.isEmpty()) {
            return EvalResult(
                    null,
                    MalList(listOf(MalSymbol("quote"), arg)),
                    env)
        }

        val head = arg.head()

        return when (head) {
            is MalSequence -> {
                val first = head.head()
                when (first) {
                    MalSymbol("unquote") -> {
                        val type = MalList(listOf(
                                MalSymbol("cons"),
                                head.tail().head(),
                                MalList(listOf(
                                        MalSymbol("quasiquote"),
                                        arg.tail()))))
                        EvalResult(null, type, env)
                    }
                    MalSymbol("splice-unquote") -> {
                        val type = MalList(listOf(
                                MalSymbol("concat"),
                                head.tail().head(),
                                MalList(listOf(
                                        MalSymbol("quasiquote"),
                                        arg.tail()))))
                        EvalResult(null, type, env)
                    }
                    else -> {
                        val type = MalList(listOf(
                                MalSymbol("cons"),
                                MalList(listOf(
                                        MalSymbol("quasiquote"),
                                        head)),
                                MalList(listOf(
                                        MalSymbol("quasiquote"),
                                        arg.tail()))))
                        EvalResult(null, type, env)
                    }
                }
            }
            MalSymbol("unquote"), MalSymbol("splice-unquote") -> {
                val type = arg.tail().head()
                EvalResult(null, type, env)
            }
            else -> {
                val type = MalList(listOf(
                        MalSymbol("cons"),
                        MalList(listOf(
                                MalSymbol("quasiquote"),
                                head)),
                        MalList(listOf(
                                MalSymbol("quasiquote"),
                                arg.tail()))))
                EvalResult(null, type, env)
            }
        }
    }

    override fun toString() = toString(false)

    override fun toString(readably: Boolean) = elements.joinToString(
            separator = " ", prefix = "(", postfix = ")") { it.toString(readably) }

}

class MalVector(elements: List<MalType>) : MalSequence(elements) {

    override fun evalInternal(env: Env): EvalResult =
            MalVector(elements.map { it.eval(env) }).toResult()

    override fun toString() = toString(false)

    override fun toString(readably: Boolean) = elements.joinToString(
            separator = " ", prefix = "[", postfix = "]") { it.toString(readably) }

}

class MalHashMap(private val keys: List<MalType>, private val values: List<MalType>) : MalType() {

    override fun evalInternal(env: Env) =
            MalHashMap(keys, values.map { it.eval(env)}).toResult()

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

class MalNumber(public val value: Int) : MalType() {

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

    override fun evalInternal(env: Env) = try {
            env[value].toResult()
        } catch (error: ValueNotFound) {
            MalError(error.toString()).toResult()
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

class MalFunction(public var isMacro: Boolean = false,
                  private val callable: (List<MalType>) -> EvalResult) : MalType() {

    companion object {
        fun builtin(callable: (List<MalType>) -> MalType) =
                MalFunction { callable(it).toResult() }
    }

    override fun toString() = "#<function>"

    fun apply(args: List<MalType>) = callable(args)

    fun applyWithResult(args: List<MalType>) : MalType {
        val evalResult = apply(args)
        return if (evalResult.result != null)
            evalResult.result
        else
            evalResult.type!!.eval(evalResult.env!!)
    }

}

class MalAtom(var value: MalType) : MalType() {

    override fun toString(readably: Boolean) =
            "(atom ${value.toString(readably)})"

}

class MalError(private val errorMsg: String,
               private val exception: MalException? = null) : MalType() {

    companion object {
        fun fromException(exception: MalException) =
                MalError("", exception)
    }

    override fun toString(readably: Boolean) =
            exception?.type?.toString(readably) ?:
            MalString(errorMsg).toString(readably)

}
