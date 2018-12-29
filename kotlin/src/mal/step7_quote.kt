package mal

fun initGlobalEnv() : Env {

    val ret = Env()
    for ((key, fn) in ns) {
        ret[key] = fn
    }

    ret["not"] = eval(read("(def! not (fn* (a) (if a false true)))"), ret)
    ret["load-file"] = eval(read("""
        (def! load-file
                (fn* (f)
                    (eval
                        (read-string
                            (str "(do " (slurp f) ")")))))
    """.trimIndent()), ret)
    ret["eval"] = MalFunction.builtin { eval(it, ret) }

    return ret
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

    val test = false

    if (test) {

        val code = """
            (quasiquote (unquote 7))
        """.trimIndent()
        println(rep(code))

    } else {

        if (args.isNotEmpty()) {
            val filePath = args[0]
            rep("(load-file \"$filePath\")")
        }

        val arguments = if (args.size > 1)
            args.sliceArray(1 until args.size).map { read(it)[0] }
        else
            emptyList()
        replEnv["*ARGV*"] = MalList(arguments)

        while(true) {

            print("user> ")
            val input = readLine()
            if (input != null) {
                val result = rep(input)
                println(result)
            }

        }

    }

}
