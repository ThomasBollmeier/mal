package mal

fun printStr(obj: MalType, printReadably: Boolean = true) : String {
    return if (printReadably) {
        var text = obj.toString().replace("\\", "\\\\")
        text = text.replace("\"", "\\\"")
        text = text.replace("\n", "\\n")
        text
    } else obj.toString()
}