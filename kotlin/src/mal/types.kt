package mal

open class MalType

class MalList(private val elements: List<MalType>) : MalType() {

    override fun toString() = elements.map { it.toString() }.joinToString(
                separator = " ", prefix = "(", postfix = ")")

}

class MalNumber(private val value: Int) : MalType() {

    override fun toString() = value.toString()

}

class MalSymbol(private val value: String) : MalType() {

    override fun toString() = value

}