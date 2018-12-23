package mal

fun initGlobalEnv() : Env {

    val ret = Env()
    for ((key, fn) in ns) {
        ret[key] = fn
    }

    ret["not"] = eval(read("(def! not (fn* (a) (if a false true)))"), ret)

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

        /*
        var code = """
            (def! fib
                (fn* (N)
                    (if (= N 0)
                        1
                        (if (= N 1)
                            1
                            (+ (fib (- N 1)) (fib (- N 2)))))))
            (fib 2)
        """.trimIndent()
        println(rep(code))

        code = """
            (do
                (def! x 41)
                (+ x 1))
        """.trimIndent()
        println(rep(code))

        code = "(if true (+ 1 41) 23)"
        println(rep(code))

        code = "((fn* [a b] (* a b)) 6 7)"
        println(rep(code))

        code = """
            (pr-str "abc")
        """.trimIndent()
        println(rep(code))

        code = """
            (def! sumdown (fn* (N) (if (> N 0) (+ N (sumdown  (- N 1))) 0)))
            (sumdown 1)
        """.trimIndent()
        println(rep(code))

        code = """
            "\n"
        """.trimIndent()
        println(rep(code))
        */

        val code = """
            "\n"
        """.trimIndent()
        println(rep(code))

    } else {

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
