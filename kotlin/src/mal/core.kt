package mal

import java.io.File

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
                if (arg is MalSequence)
                        MalNumber(arg.elements.size)
                else
                    MalNumber(0)
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
