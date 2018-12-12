package mal

enum class TokenType {
    LIST_BEGIN, LIST_END, OTHER
}

data class Token(val type: TokenType, val value: String)

class Reader(private val tokens: List<Token>) {

    private var currPos = 0

    fun next() : Token? = if (currPos < tokens.size) {
        tokens[currPos++]
    } else null

    fun peek() : Token? = if (currPos < tokens.size) {
        tokens[currPos]
    } else null

    fun readForm() : MalType? {

        val token = peek()
        if (token == null) {
            return null
        }

        return when (token.type) {
            TokenType.LIST_BEGIN -> readList()
            else -> readAtom(token)
        }

    }

    fun readList() : MalList? {

        next()

        val elements = mutableListOf<MalType>()

        while (true) {

            var token = peek()
            if (token == null) return null

            var element = when (token.type) {
                TokenType.LIST_END -> return MalList(elements)
                else -> readForm()
            }

            if (element == null) return null
            elements += element

        }

    }

    fun readAtom(token: Token) : MalType {
        next()
        val number = token.value.toIntOrNull()
        return when (number) {
            null -> MalSymbol(token.value)
            else -> MalNumber(number)
        }

    }

}

fun readStr(code: String) : MalType? {

    val tokens = tokenize(code)
    val reader = Reader(tokens)
    return reader.readForm()

}

fun tokenize(code: String) : List<Token> {

    val pattern = """
        [\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"|;.*|[^\s\[\]{}('"`,;)]*)
    """.trimIndent()
    val regex = Regex(pattern)
    val result = mutableListOf<Token>()

    for (matchResult in regex.findAll(code)) {

        val value = matchResult.value.trim()
        if (value.isEmpty()) continue

        val type = when (value) {
            "(" -> TokenType.LIST_BEGIN
            ")" -> TokenType.LIST_END
            else -> TokenType.OTHER
        }
        result += Token(type, value)

    }

    return result
}