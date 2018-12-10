package mal

fun READ(s: String) = s

fun EVAL(s: String) = s

fun PRINT(s: String)  = s

fun rep(s: String) = PRINT(EVAL(READ(s)))


fun main(args: Array<String>) {

    val done = false

    while(!done) {

        print("user> ")
        var input = readLine()
        if (input != null) {
            var result = rep(input)
            println(result)
        }

    }

}
