package mal

fun printStr(obj: MalType, printReadably: Boolean = true) : String {
    return if (printReadably) {
        var text = obj.toString()
        if (obj is MalString) text = text.trim('"')
        text = text.replace("\\", "\\\\")
        text = text.replace("\"", "\\\"")
        text = text.replace("\n", "\\n")
        if (obj is MalString) "\"$text\"" else text
    } else obj.toString()
}