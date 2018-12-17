package mal

typealias Env = HashMap<String, MalType>

val replEnv : Env = hashMapOf(
        "+" to MalFunction add@ { xs ->
            if (xs.all { it is MalNumber }) {
                val sum = xs.map { it as MalNumber }
                        .map { it.value }.sum()
                return@add MalNumber(sum)
            } else MalError("unsupported operation")
        },
        "-" to MalFunction sub@ { xs ->
            if (xs.all { it is MalNumber }) {
                val numbers = xs.map { it as MalNumber }
                        .map { it.value }
                return@sub MalNumber(numbers.reduce {a, b -> a - b})
            } else MalError("unsupported operation")
        },
        "*" to MalFunction prod@ { xs ->
            if (xs.all { it is MalNumber }) {
                val numbers = xs.map { it as MalNumber }
                        .map { it.value }
                return@prod MalNumber(numbers.reduce {a, b -> a * b})
            } else MalError("unsupported operation")
        },
        "/" to MalFunction quot@ { xs ->
            if (xs.all { it is MalNumber }) {
                val numbers = xs.map { it as MalNumber }
                        .map { it.value }
                return@quot MalNumber(numbers.reduce {a, b -> a / b})
            } else MalError("unsupported operation")
        }
)

fun read(s: String) = readStr(s)

fun eval(ast: MalType, env: Env) : MalType {
    return when(ast) {
        !is MalList -> evalAst(ast, env)
        else -> if (!ast.isEmpty()) {
            val evaluated = evalAst(ast, env) as MalList
            val fn = evaluated.elements[0]
            val numArgs = evaluated.elements.size - 1
            val args = (if (numArgs > 0)
                evaluated.elements.subList(1, numArgs + 1)
                else emptyList()).toTypedArray()
            if (fn is MalFunction)
                fn.call(args)
                else MalError("expected function")
        } else ast
    }
}

fun evalAst(ast: MalType, env: Env) : MalType =
    when (ast) {
        is MalSymbol -> env.getOrDefault(ast.value,
                MalError("${ast.value} not found"))
        is MalList -> {
            val evaluated = ast.elements.map { evalAst(it, env) }
            MalList(evaluated)
        }
        else -> ast
    }

fun print(obj: MalType)  = printStr(obj)

fun rep(s: String) : String = print(eval(read(s), replEnv))


fun main(args: Array<String>) {

    while(true) {

        print("user> ")
        var input = readLine()
        if (input != null) {
            var result = rep(input)
            println(result)
        }

    }

}
