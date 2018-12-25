package mal

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
