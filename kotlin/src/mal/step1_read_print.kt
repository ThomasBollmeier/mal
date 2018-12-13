package mal

fun read(s: String) = readStr(s)

fun eval(obj: MalType) = obj

fun print(obj: MalType)  = printStr(obj)

fun rep(s: String) : String {
    val obj = read(s)
    return if (obj != null) {
        print(eval(obj))
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
