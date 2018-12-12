package mal

fun READ(s: String) = readStr(s)

fun EVAL(obj: MalType) = obj

fun PRINT(obj: MalType)  = printStr(obj)

fun rep(s: String) : String {
    val obj = READ(s)
    return if (obj != null) {
        PRINT(EVAL(obj))
    } else ""
}


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
