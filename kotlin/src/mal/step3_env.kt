package mal

fun initGlobalEnv() : Env {

    val ret = Env()

    ret["+"] = createArithmeticFun {a, b -> a + b}
    ret["-"] = createArithmeticFun {a, b -> a - b}
    ret["*"] = createArithmeticFun {a, b -> a * b}
    ret["/"] = createArithmeticFun {a, b -> a / b}

    return ret
}

fun createArithmeticFun(binOp: (Int, Int) -> Int) =
    MalFunction multOp@ { xs ->
        if (xs.all { it is MalNumber }) {
            val numbers = xs.map { it as MalNumber }
                    .map { it.value }
            return@multOp MalNumber(numbers.reduce(binOp))
        } else MalError("unsupported operation" )
    }

val replEnv = initGlobalEnv()

fun read(s: String) = readStr(s)

fun eval(types: List<MalType>, env: Env) : MalType {
    var result : MalType = MalNil()
    for (type in types) {
        result = type.eval(env)
    }
    return result
}

fun print(obj: MalType)  = printStr(obj)

fun rep(s: String) : String = print(eval(read(s), replEnv))


fun main(args: Array<String>) {

    /*
    var code = """
        (def! x 41)
        (+ x 1)
    """.trimIndent()
    println(rep(code))

    code = "(let* (x 22 y 20) (+ x y))"
    println(rep(code))
    */

    while(true) {

        print("user> ")
        var input = readLine()
        if (input != null) {
            var result = rep(input)
            println(result)
        }

    }

}
