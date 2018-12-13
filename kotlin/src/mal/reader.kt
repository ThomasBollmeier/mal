package mal

enum class TokenType {
    LPAR,
    RPAR,
    LSQBR,
    RSQBR,
    LBRACE,
    RBRACE,
    NUMBER,
    STRING,
    NIL,
    TRUE,
    FALSE,
    OTHER
}

data class Token(val type: TokenType, val value: String)

class Reader(private val tokens: List<Token>) {

    private var currPos = 0

    private fun next() : Token? = if (currPos < tokens.size) {
        tokens[currPos++]
    } else null

    private fun peek() : Token? = if (currPos < tokens.size) {
        tokens[currPos]
    } else null

    fun readForm() : MalType {

        val token = peek() ?: return MalError("EOF")

        return when (token.type) {
            TokenType.LPAR, TokenType.LSQBR, TokenType.LBRACE -> readCollection(token.type)
            else -> readAtom(token)
        }

    }

    private fun readCollection(startType: TokenType) : MalType {

        next()

        val elements = mutableListOf<MalType>()
        val endType = when(startType) {
            TokenType.LPAR -> TokenType.RPAR
            TokenType.LSQBR -> TokenType.RSQBR
            TokenType.LBRACE -> TokenType.RBRACE
            else -> TokenType.OTHER
        }

        while (true) {

            val token = peek() ?: return MalError("EOF")

            val element = when (token.type) {
                endType -> return MalList(elements)
                else -> readForm()
            }

            if (element is MalError) return element
            elements += element

        }

    }

    private fun readAtom(token: Token) : MalType {

        next()

        return when (token.type) {
            TokenType.NUMBER -> MalNumber(token.value.toInt())
            else -> MalSymbol(token.value)
        }

    }

}

fun readStr(code: String) : MalType {

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

        val value = matchResult.groupValues[1]

        val type = when {
            value == "(" -> TokenType.LPAR
            value == ")" -> TokenType.RPAR
            value == "[" -> TokenType.LSQBR
            value == "]" -> TokenType.RSQBR
            value == "{" -> TokenType.LBRACE
            value == "}" -> TokenType.RBRACE
            value.isNumber() -> TokenType.NUMBER
            value.isString() -> TokenType.STRING
            value == "nil" -> TokenType.NIL
            value == "true" -> TokenType.TRUE
            value == "false" -> TokenType.FALSE
            else -> TokenType.OTHER
        }
        result += Token(type, value)

    }

    return result
}

fun String.isNumber() = this.toIntOrNull() != null

fun String.isString() : Boolean = this.isNotEmpty() && this[0] == '"'
