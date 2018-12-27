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
            else -> readSimple(token)
        }

    }

    fun readForms() : List<MalType> {
        val result = mutableListOf<MalType>()
        do {
            result += readForm()
        } while (peek() != null)
        return result
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
                endType -> {
                    next()
                    return when (endType) {
                        TokenType.RPAR -> MalList(elements)
                        TokenType.RSQBR -> MalVector(elements)
                        TokenType.RBRACE -> createHashMap(elements)
                        else -> MalError("Unexpected token type")
                    }
                }
                else -> readForm()
            }

            if (element is MalError) return element
            elements += element

        }

    }

    private fun createHashMap(elements: List<MalType>) : MalType {

        if (elements.size % 2 != 0) return MalError("Numbers of keys and values do not match")

        val keys = mutableListOf<MalType>()
        val values = mutableListOf<MalType>()

        for ((i, element) in elements.withIndex()) {
            if (i % 2 == 0) keys += element else values += element
        }

        return MalHashMap(keys, values)
    }

    private fun readSimple(token: Token) : MalType {

        next()

        return when (token.type) {
            TokenType.NUMBER -> MalNumber(token.value.toInt())
            TokenType.STRING -> readString(token)
            TokenType.TRUE -> MalBoolean(true)
            TokenType.FALSE -> MalBoolean(false)
            TokenType.NIL -> MalNil()
            else -> if (token.value.isNotEmpty() && token.value[0] == ':')
                    MalKeyword(token.value)
                else
                    MalSymbol(token.value)
        }

    }

    private fun readString(token: Token) : MalType {

        val lastChar = token.value[token.value.length - 1]
        return if (lastChar == '"')
            MalString(MalString.unquote(token.value))
        else
            MalError("No end of string (\") found")

    }

}

fun readStr(code: String) : List<MalType> {

    val tokens = tokenize(code)
    val reader = Reader(tokens)
    return reader.readForms()
}

fun tokenize(code: String) : List<Token> {

    val pattern = """
        [\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"|;.*|[^\s\[\]{}('"`,;)]*)
    """.trimIndent()
    val regex = Regex(pattern)
    val result = mutableListOf<Token>()

    for (matchResult in regex.findAll(code)) {

        val value = matchResult.groupValues[1]
        if (value.isBlank() || value.isComment()) continue

        val type = when(value) {
            "(" -> TokenType.LPAR
            ")" -> TokenType.RPAR
            "[" -> TokenType.LSQBR
            "]" -> TokenType.RSQBR
            "{" -> TokenType.LBRACE
            "}" -> TokenType.RBRACE
            "nil" -> TokenType.NIL
            "true" -> TokenType.TRUE
            "false" -> TokenType.FALSE
            else -> when {
                value.isNumber() -> TokenType.NUMBER
                value.isString() -> TokenType.STRING
                else -> TokenType.OTHER
            }
        }
        result += Token(type, value)

    }

    return result
}

fun String.isNumber() = this.toIntOrNull() != null

fun String.isString() : Boolean = this.isNotEmpty() && this[0] == '"'

fun String.isComment() = this.isNotEmpty() && this[0] == ';'
