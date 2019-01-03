package mal

import java.io.File
import java.util.*

class IllegalArgumentError : Exception()

val ns = hashMapOf(
        "+" to createArithmeticFun { a, b -> a + b},
        "-" to createArithmeticFun { a, b -> a - b},
        "*" to createArithmeticFun { a, b -> a * b},
        "/" to createArithmeticFun { a, b -> a / b},
        "=" to MalFunction.builtin { args ->
            MalBoolean(args.size == 2 && args[0] == args[1])
        },
        "<" to createComparisonFun { a, b -> a < b },
        "<=" to createComparisonFun { a, b -> a <= b },
        ">" to createComparisonFun { a, b -> a > b },
        ">=" to createComparisonFun { a, b -> a >= b },
        "prn" to MalFunction.builtin { args ->
            args.forEach { println(printStr(it, true)) }
            MalNil()
        },
        "list" to MalFunction.builtin { args -> MalList(args) },
        "list?" to MalFunction.builtin { args ->
            MalBoolean(args.size == 1 && args[0] is MalList)
        },
        "empty?" to MalFunction.builtin { args ->
            if (args.size == 1) {
                val arg = args[0]
                MalBoolean(arg is MalSequence && arg.isEmpty())
            } else
                MalBoolean(false)
        },
        "count" to MalFunction.builtin { args ->
            if (args.size == 1) {
                val arg = args[0]
                when (arg) {
                    is MalSequence -> MalNumber(arg.elements.size)
                    is MalHashMap -> arg.count()
                    else -> MalNumber(0)
                }
            } else
                MalNumber(0)
        },
        "pr-str" to MalFunction.builtin { args ->
            val s = args.joinToString(" ") { printStr(it, true) }
            MalString(s)
        },
        "str" to MalFunction.builtin { args ->
            val s = args.joinToString("") { it.toString() }
            MalString(s)
        },
        "prn" to MalFunction.builtin { args ->
            println(args.joinToString(" ") { printStr(it, true) })
            MalNil()
        },
        "println" to MalFunction.builtin { args ->
            println(args.joinToString(" ") { printStr(it, false) })
    MalNil()
        },
        "read-string" to MalFunction.builtin {
            val s = if (it.size == 1)
                it[0] as? MalString ?: throw IllegalArgumentError()
            else
                throw IllegalArgumentError()
            val types = readStr(s.value)

            if (types.isNotEmpty())
                types.last()
            else
                MalNil()
        },
        "slurp" to MalFunction.builtin {
            val filePath = if (it.size == 1)
                it[0] as? MalString ?: throw IllegalArgumentError()
            else
                throw IllegalArgumentError()

            MalString(File(filePath.value).readText())
        },
        "atom" to MalFunction.builtin {
            MalAtom(it[0])
        },
        "atom?" to MalFunction.builtin {
            MalBoolean(it[0] is MalAtom)
        },
        "deref" to MalFunction.builtin {
            val atom = it[0] as? MalAtom ?: throw IllegalArgumentError()
            atom.value
        },
        "reset!" to MalFunction.builtin {
            if (it.size != 2) throw IllegalArgumentError()
            val atom = it[0] as? MalAtom ?: throw IllegalArgumentError()
            atom.value = it[1]
            atom.value
        },
        "swap!" to MalFunction.builtin {
            if (it.size < 2) throw IllegalArgumentError()
            val atom = it[0] as? MalAtom ?: throw IllegalArgumentError()
            val fn = it[1] as? MalFunction ?: throw IllegalArgumentError()
            val args = mutableListOf(atom.value)
            if (it.size > 2) args.addAll(it.subList(2, it.size))
            atom.value = fn.applyWithResult(args)
            atom.value
        },
        "cons" to MalFunction.builtin {
            val head = it[0]
            val seq = it[1] as? MalSequence?: throw IllegalArgumentError()
            seq.cons(head)
        },
        "concat" to MalFunction.builtin {
            if (!it.all { lst -> lst is MalSequence }) throw IllegalArgumentError()

            val seqs = it.map { seq -> seq as MalSequence }.toTypedArray()
            MalSequence.concat(*seqs)
        },
        "nth" to MalFunction.builtin lambda@ {
            if (it.size != 2) return@lambda MalError("Illegal #args")
            val seq = it[0] as? MalSequence ?: return@lambda MalError("list expected")
            val idx = it[1] as? MalNumber ?: return@lambda MalError("number expected")
            seq.nth(idx.value)
        },
        "first" to MalFunction.builtin lambda@ {
            if (it.size != 1) return@lambda MalError("Illegal #args")
            val seq = it[0] as? MalSequence ?:
                return@lambda if (it[0] is MalNil)
                    MalNil()
                else
                    MalError("list expected")
            seq.head()
        },
        "rest" to MalFunction.builtin lambda@ {
            if (it.size != 1) return@lambda MalError("Illegal #args")
            val seq = it[0] as? MalSequence ?:
                return@lambda if (it[0] is MalNil)
                    MalList(emptyList())
                else
                    MalError("list expected")
            seq.tail()
        },
        "throw" to MalFunction.builtin {
            throw MalException(it[0])
        },
        "apply" to MalFunction.builtin lambda@{
            val fn = it[0] as? MalFunction ?:
                return@lambda MalError("function expected")
            val lastArg = it.last() as MalSequence
            val args = mutableListOf<MalType>()
            for ((i, type) in it.withIndex()) {
                if (i > 0 && i < it.size - 1) {
                    args += type
                }
            }
            args.addAll(lastArg.elements)
            fn.applyWithResult(args)
        },
        "map" to MalFunction.builtin lambda@{
            val fn = it[0] as? MalFunction ?:
                return@lambda MalError("function expected")
            val seq = it[1] as? MalSequence ?:
                return@lambda MalError("sequence expected")

            val mapped = seq.elements.map {
                fn.applyWithResult(listOf(it))
            }

            MalList(mapped)
        },
        "nil?" to MalFunction.builtin {
            MalBoolean(it[0] is MalNil)
        },
        "true?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalBoolean && type.value)
        },
        "false?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalBoolean && !type.value)
        },
        "symbol?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalSymbol)
        },
        "symbol" to MalFunction.builtin {
            val s = it[0] as MalString
            MalSymbol(s.value)
        },
        "keyword?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalKeyword)
        },
        "keyword" to MalFunction.builtin {
            val s = it[0] as MalString
            MalKeyword(":${s.value}")
        },
        "vector" to MalFunction.builtin {
            MalVector(it)
        },
        "vector?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalVector)
        },
        "sequential?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalSequence)
        },
        "map?" to MalFunction.builtin {
            val type = it[0]
            MalBoolean(type is MalHashMap)
        },
        "hash-map" to MalFunction.builtin lambda@{
            if (it.size % 2 != 0)
                return@lambda MalError("#args must be even")

            val keys = mutableListOf<MalType>()
            val values = mutableListOf<MalType>()

            it.withIndex().forEach { (i, type) ->
                if (i % 2 == 0)
                    keys += type
                else
                    values += type
            }

            MalHashMap(keys, values)
        },
        "assoc" to MalFunction.builtin lambda@{
            if (it.size % 2 != 1)
                return@lambda MalError("#args must be odd")
            if (it[0] !is MalHashMap)
                return@lambda MalError("first arg must be a hash map")

            val map = it[0] as MalHashMap
            val keys = mutableListOf<MalType>()
            val values = mutableListOf<MalType>()

            it.drop(1).withIndex().forEach { (i, type) ->
                if (i % 2 == 0)
                    keys += type
                else
                    values += type
            }

            map.assoc(keys, values)
        },
        "dissoc" to MalFunction.builtin lambda@{
            if (it[0] !is MalHashMap)
                return@lambda MalError("first arg must be a hash map")

            val map = it[0] as MalHashMap
            val keys = it.drop(1)

            map.dissoc(keys)
        },
        "get" to MalFunction.builtin lambda@{
            if (it.size != 2)
                return@lambda MalError("two args expected")
            val first = it[0]
            when (first) {
                is MalHashMap -> first.get(it[1])
                else -> MalNil()
            }
        },
        "contains?" to MalFunction.builtin lambda@{
            if (it.size != 2)
                return@lambda MalError("two args expected")
            if (it[0] !is MalHashMap)
                return@lambda MalError("first arg must be a hash map")

            val map = it[0] as MalHashMap

            MalBoolean(map.hasKey(it[1]))
        },
        "keys" to MalFunction.builtin lambda@{
            if (it.size != 1)
                return@lambda MalError("one arg expected")
            if (it[0] !is MalHashMap)
                return@lambda MalError("arg must be a hash map")

            val map = it[0] as MalHashMap

            map.keys()
        },
        "vals" to MalFunction.builtin lambda@{
            if (it.size != 1)
                return@lambda MalError("one arg expected")
            if (it[0] !is MalHashMap)
                return@lambda MalError("arg must be a hash map")

            val map = it[0] as MalHashMap

            map.values()
        },
        "readline" to MalFunction.builtin lambda@{
            if (it.size != 1)
                return@lambda MalError("one arg expected")
            val s = it[0] as? MalString ?:
                return@lambda MalError("arg must be a string")
            print(s.toString())
            val input = readLine()
            if (input != null)
                MalString(input)
            else
                MalNil()

        },
        "meta" to MalFunction.builtin lambda@{
            if (it.size != 1)
                return@lambda MalError("one arg expected")
            val arg = it[0]
            when (arg) {
                is MalMetaDataContainer<*> -> arg.meta
                else -> MalError("metadata container expected")
            }
        },
        "with-meta" to MalFunction.builtin lambda@{
            if (it.size != 2)
                return@lambda MalError("two args expected")
            val arg = it[0]
            when (arg) {
                is MalMetaDataContainer<*> -> arg.withMeta(it[1])
                else -> MalError("metadata container expected")
            }
        },
        "time-ms" to MalFunction.builtin {
            MalNumber(Date().time.toInt())
        },
        "conj" to MalFunction.builtin lambda@{
            if (it.size < 2)
                return@lambda MalError("at least two args expected")
            if (it[0] !is MalSequence)
                return@lambda MalError("first arg must be a sequence")

            when (it[0]) {
                is MalList -> {
                    it.drop(1).fold(it[0] as MalList) {
                                acc, elem -> acc.cons(elem)
                    }
                }
                is MalVector -> {
                    val elements = MalSequence.concat(
                            it[0] as MalVector,
                            MalList(it.drop(1))
                            ).elements
                    MalVector(elements)
                }
                else -> MalError("unsupported type")
            }

        },
        "string?" to MalFunction.builtin {
            MalBoolean(it.size == 1 && it[0] is MalString)
        },
        "number?" to MalFunction.builtin {
            MalBoolean(it.size == 1 && it[0] is MalNumber)
        },
        "fn?" to MalFunction.builtin {
            MalBoolean(it.size == 1 && it[0] is MalFunction
                    && !(it[0] as MalFunction).isMacro)
        },
        "macro?" to MalFunction.builtin {
            MalBoolean(it.size == 1 && it[0] is MalFunction
                && (it[0] as MalFunction).isMacro)
        },
        "seq" to MalFunction.builtin lambda@{
            if (it.size != 1)
                return@lambda MalNil()

            val arg = it[0]

            when (arg) {
                is MalList -> if (!arg.isEmpty())
                    arg
                else
                    MalNil()
                is MalVector -> if (!arg.isEmpty())
                    MalList(arg.elements)
                else
                    MalNil()
                is MalString -> {
                    val s = arg.value
                    val elements = mutableListOf<MalType>()
                    for (ch in s) {
                        elements += MalString(ch.toString())
                    }
                    if (elements.isNotEmpty())
                        MalList(elements)
                    else
                        MalNil()
                }
                else -> MalNil()
            }
        }
)

fun createArithmeticFun(binOp: (Int, Int) -> Int) =
        MalFunction.builtin multOp@ { xs ->
            if (xs.all { it is MalNumber }) {
                val numbers = xs.map { it as MalNumber }.map { it.value }
                return@multOp MalNumber(numbers.reduce(binOp))
            } else MalError("unsupported operation" )
        }

fun createComparisonFun(binOp: (Int, Int) -> Boolean) =
        MalFunction.builtin multOp@ { xs ->
            if (xs.all { it is MalNumber }) {
                val numbers = xs.map { it as MalNumber
                }.map { it.value }
                if (numbers.size <= 1) return@multOp MalBoolean(true)
                val n = numbers.size
                val pairs = numbers.zip(numbers.subList(1, n))
                return@multOp MalBoolean(pairs.all { binOp(it.first, it.second) })
            } else MalError("unsupported operation" )
        }
