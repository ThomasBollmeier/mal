package mal

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

fun eval(ast: MalType, env: Env) = ast.eval(env)

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
