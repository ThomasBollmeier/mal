package mal

fun printStr(obj: MalType, printReadably: Boolean = true) : String {
    return if (printReadably && obj is MalString) {
        MalString.escape(obj.toString())
    } else obj.toString()
}