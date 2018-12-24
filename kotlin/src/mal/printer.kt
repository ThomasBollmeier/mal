package mal

fun printStr(obj: MalType, printReadably: Boolean = true) =
        obj.toString(printReadably)
