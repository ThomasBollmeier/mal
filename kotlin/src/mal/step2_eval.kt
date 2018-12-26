package mal2

import mal.*

fun initGlobalEnv() : Env {

    val ret = Env()

    ret["+"] = createArithmeticFun {a, b -> a + b}
    ret["-"] = createArithmeticFun {a, b -> a - b}
    ret["*"] = createArithmeticFun {a, b -> a * b}
    ret["/"] = createArithmeticFun {a, b -> a / b}

    return ret
}

fun createArithmeticFun(binOp: (Int, Int) -> Int) =
        MalFunction.builtin multOp@ { xs ->
            if (xs.all { it is MalNumber }) {
                val numbers = xs.map { it as MalNumber }
                        .map { it.value }
                return@multOp MalNumber(numbers.reduce(binOp))
            } else MalError("unsupported operation" )
        }

val replEnv = initGlobalEnv()

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
