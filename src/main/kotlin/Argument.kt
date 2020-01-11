package dev.mee42

class ParseError private constructor(val message: String){
    companion object {
        fun of(arguments: Array<String>?, message_: String, index: Int? = null): ParseError {
            if(arguments == null || arguments.isEmpty() || index == null) return ParseError(message_)
            var str = ""
            var realIndex = -1
            for((i,arg) in arguments.withIndex()) {
                str += " $arg"
                if(i == index) {
                    realIndex = str.length - (arg.length / 2)
                }
            }
            str = str.substring(1)
            str += "\n"
            val (here, off) = if(realIndex > 10) "here: ^" to "here ^".length else "^ :here" to 0
            if(realIndex == -1) error("Can't find argument with index $index: $arguments")
            val offset = CharArray(realIndex - off) { ' ' }.fold("") { a,b -> "$a$b" }
            str += offset + here
            str += "\n"
            str += "$offset   $message_"
            return ParseError(str)
        }
    }

    override fun toString(): String {
        return "\n$message"
    }
}

class ArgumentParser private constructor(val fields: List<Argument>){
    companion object {
        fun of(block: ArgumentParserBuilder.() -> Unit): ArgumentParser {
            return ArgumentParser(ArgumentParserBuilder().apply(block).build())
        }
    }
    fun parse(arguments: Array<String>): PossibleValue<ParsedArguments, ParseError> {
        val parsed = mutableMapOf<Argument,String?>()
        val free = mutableListOf<String>()
        var index = 0
        var waitingFor :Argument? = null
        var isWaitingForLong: Boolean = false
        while(index < arguments.size) {
            val arg = arguments[index]
            when {
                arg.startsWith("--") -> {
                    if(waitingFor != null) return ParseError.of(arguments, "Found when looking for value for " +
                                if(isWaitingForLong) "--${waitingFor.long}" else "-${waitingFor.short}", index).asFail()
                    val name = arg.substring("--".length)
                    val field = fields.firstOrNull { it.long == name }
                        ?: return ParseError.of(arguments, "Can't find this field", index).asFail()
                    if(field.needsArgument) {
                        waitingFor = field
                        isWaitingForLong = true
                    } else {
                        parsed[field] = null
                    }
                }
                arg.startsWith("-") -> {
                    val charString = arg.substring("-".length)
                    for((i, char) in charString.withIndex()) {
                        val field = fields.firstOrNull { it.short == char }
                            ?: return ParseError.of(arguments, "Can't find this field", index).asFail()
                        if(field.needsArgument) {
                            if (i == charString.length - 1) {
                                // if it's the last one
                                waitingFor = field
                                isWaitingForLong = false
                            } else {
                                return ParseError.of(arguments, "'-$char' needs a value and can't be in a flag block", index).asFail()
                            }
                        } else {
                            parsed[field] = null
                        }
                    }
                }
                waitingFor != null -> {
                    // this is the free we're waiting for
                    parsed[waitingFor] = arg
                    waitingFor = null
                }
                else -> {
                    free.add(arg)
                }
            }
            index++
        }
        if(waitingFor != null) {
            return ParseError.of(null,
                "never found the value for " + if(isWaitingForLong) "--${waitingFor.long}" else "-${waitingFor.short}",
                null).asFail()
        }

        return ParsedArguments(parsed, free).asSuccess()
    }

}

class ParsedArguments (val parsed: Map<Argument,String?>,val free: List<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParsedArguments

        if (parsed != other.parsed) return false
        if (free != other.free) return false

        return true
    }


    override fun hashCode(): Int {
        var result = parsed.hashCode()
        result = 31 * result + free.hashCode()
        return result
    }

    override fun toString(): String {
        return "ParsedArguments($parsed, free=$free)"
    }

    fun isFlagSpecified(name_: String): Boolean {
        val name = if(name_.startsWith("--")) name_.substring(2) else name_
        val yes = parsed.toList().firstOrNull { it.first.long == name } ?: return false
        if(yes.first.needsArgument) error("This isn't a boolean field!")
        if(yes.second != null) error("This shouldn't be null")
        return true
    }
    fun getArg(name_: String): String? {
        val name = if(name_.startsWith("--")) name_.substring(2) else name_
        return parsed.toList().firstOrNull { it.first.long == name }?.second
    }
}

class Argument(val short: Char?, val long: String, val needsArgument: Boolean) {
    override fun toString(): String {
        return "Argument{--$long" + if(needsArgument) " [arg]}" else "}"
    }
}

class ArgumentParserBuilder {
    private val list = mutableListOf<Argument>()
    fun flag(short: Char? = null, long: String){
        list.add(Argument(short,long,needsArgument = false))
    }
    fun arg(short: Char? = null, long: String){
        list.add(Argument(short, long, needsArgument = true))
    }
    fun build() : List<Argument> {
        return list
    }
}
