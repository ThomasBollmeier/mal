package mal

fun initGlobalEnv(): Env {

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
    ret["cond"] = eval(read("""
        (defmacro! cond
            (fn* (& xs)
                (if (> (count xs) 0)
                    (list 'if (first xs)
                        (if (> (count xs) 1)
                            (nth xs 1)
                            (throw "odd number of forms to cond"))
                        (cons 'cond
                            (rest (rest xs)))))))
    """.trimIndent()), ret)
    ret["or"] = eval(read("""or
        (def! *gensym-counter* (atom 0))
        (def! gensym (fn* []
            (symbol
                (str "G__"
                    (swap! *gensym-counter*
                        (fn* [x] (+ 1 x)))))))
        (defmacro! or
            (fn* (& xs)
                (if (empty? xs)
                    nil
                    (if (= 1 (count xs))
                        (first xs)
                        (let* (condvar (gensym))
                            `(let* (~condvar ~(first xs))
                                (if ~condvar
                                    ~condvar
                                    (or ~@(rest xs)))))))))
    """.trimIndent()), ret)
    ret["*host-language*"] = MalString("kotlin")

    return ret
}


val replEnv = initGlobalEnv()

fun read(s: String) = readStr(s)

fun eval(types: List<MalType>, env: Env): MalType {
    var result: MalType = MalNil()
    for (type in types) {
        result = type.eval(env)
    }
    return result
}

fun print(obj: MalType) = printStr(obj)

fun rep(s: String): String = print(eval(read(s), replEnv))


fun main(args: Array<String>) {

    if (args.isEmpty()) {
        rep("""(println (str "Mal [" *host-language* "]"))""")
    } else {
        val filePath = args[0]
        rep("(load-file \"$filePath\")")
    }

    val arguments = if (args.size > 1)
        args.sliceArray(1 until args.size).map { read(it)[0] }
    else
        emptyList()
    replEnv["*ARGV*"] = MalList(arguments)

    while (true) {

        print("user> ")
        val input = readLine()
        if (input != null) {
            println(rep(input))
        }

    }

}
