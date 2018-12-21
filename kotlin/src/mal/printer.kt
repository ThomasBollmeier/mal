package mal

fun printStr(obj: MalType, printReadably: Boolean = true) : String {
    return if (printReadably && obj is MalString) {
        var text = obj.toString()
        text = text.trim('"')
        text = text.replace("\\", "\\\\")
        text = text.replace("\"", "\\\"")
        text = text.replace("\n", "\\n")
        "\"$text\""
    } else obj.toString()
}