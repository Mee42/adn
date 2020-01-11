import arrow.core.Either
import arrow.core.Try
import arrow.core.some
import dev.mee42.Argument
import dev.mee42.ArgumentParser
import dev.mee42.ParsedArguments
import dev.mee42.PossibleValue
import io.kotlintest.*
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.specs.StringSpec
import java.util.regex.Pattern
import kotlin.Result

private val parser = ArgumentParser.of {
    flag(short = 'a', long = "AAA")
    flag(short = 'b', long = "BBB")
    flag(short = 'c', long = "CCC")
    arg(short = 'd', long = "DDD")
    arg(short = 'e', long = "EEE")
    arg(short = 'f', long = "FFF")
}


class ArgumentParsingTests: StringSpec({
    "Splitter properly splits empty strings" {
        "".splitParse() shouldBe emptyList()
    }
    "Splitter properly splits on big gaps" {
        "aa      bbbb       ccc".splitParse() shouldBe listOf("aa","bbbb","ccc")
    }
    "Empty arguments return nothing" {

        parse("").assertNotFailure() shouldBe mockit {}
    }
    "Basic single flag" {
        parse("-a").assertNotFailure() shouldBe mockit {
            flag('a')
        }
    }
    "Basic single flag long format" {
        parse("--AAA").assertNotFailure() shouldBe mockit {
            flag("AAA")
        }
    }
    "Several flags separate" {
        parse("-a -b -c").assertNotFailure() shouldBe mockit {
            flag('a')
            flag('b')
            flag('c')
        }
    }
    "Several flags together" {
        parse("-abc").assertNotFailure() shouldBe mockit {
            flag('a');flag('b');flag('c')
        }
    }
    "Using flags in long format" {
        parse("--AAA --BBB --CCC").assertNotFailure() shouldBe mockit {
            flag('a');flag('b');flag('c')
        }
    }
    "Flag order is irrelevant" {
        parse("-bca").assertNotFailure() shouldBe mockit {
            flag('a');flag('b');flag('c')
        }
    }
    "Flags can be mixed long and short" {
        parse("--BBB -a --CCC").assertNotFailure() shouldBe mockit {
            flag('a');flag('b');flag('c')
        }
    }
    "Flags work with free items" {
        parse("-a free1 -b free2 -c free3").assertNotFailure() shouldBe mockit {
            flag('a'); flag('b') ; flag('c')
            free("free1", "free2", "free3")
        }
    }
    "Value fields work for long" {
        parse("--DDD value1 --EEE value2 --FFF value3").assertNotFailure() shouldBe mockit {
            arg('d',"value1"); arg('e', "value2"); arg('f',"value3")
        }
    }
    "Value fields work for short" {
        parse("-d value1 -e value2 -f value3").assertNotFailure() shouldBe mockit {
            arg('d',"value1"); arg('e', "value2"); arg('f',"value3")
        }
    }
    "value fields work in groups" {
        parse("-ad value1").assertNotFailure() shouldBe mockit {
            arg('d',"value1")
            flag('a')
        }
    }
    "Can't find identifiers" {
        parse("-x").assertFailure().message shouldContain "Can't find this field"
        parse("--x").assertFailure().message shouldContain "Can't find this field"
    }
    "Can't use clumped value fields that isn't at the end" {
        parse("-da").assertFailure().message shouldContain "needs a value and can't be in a flag block"
    }
    "Can't end without supplying a value" {
        parse("-d").assertFailure().message shouldBe "never found the value for -d"
    }
    "Single floating value" {
        parse("floating").assertNotFailure() shouldBe mockit {
            free("floating")
        }
    }
    "Everything" {
        parse("--AAA floating1 --DDD value1 floating2 floating3 -bf value2 floating4").assertNotFailure() shouldBe mockit {
            flag('a')
            flag('b')
            arg('d',"value1")
            arg('f',"value2")
            free("floating1","floating2","floating3","floating4")
        }
    }
})



private fun mockit(block: ParsedArgumentBuilder.() -> Unit) = parser.mock(block)

private fun ArgumentParser.mock(block: ParsedArgumentBuilder.() -> Unit): ParsedArguments {
    return ParsedArgumentBuilder(this).apply(block).build()
}

private class ParsedArgumentBuilder(private val parser: ArgumentParser){
    fun build(): ParsedArguments {
        return ParsedArguments(arguments, freeStr ?: emptyList())
    }
    private val arguments = mutableMapOf<Argument, String?>()

    fun flag(name: Char) {
        val field = parser.fields.firstOrNull { it.short == name } ?: error("Can't find $name flag")
        if(field.needsArgument) error("$name is not a flag")
        arguments[field] = null
    }
    fun flag(name: String) {
        val field = parser.fields.firstOrNull { it.long == name } ?: error("Can't find $name flag")
        if(field.needsArgument) error("$name is not a flag")
        arguments[field] = null
    }
    fun arg(name: Char,value: String) {
        val field = parser.fields.firstOrNull { it.short == name } ?: error("Can't find $name value")
        if(!field.needsArgument) error("$name is a flag")
        arguments[field] = value
    }
    fun arg(name: String, value: String){
        val field = parser.fields.firstOrNull { it.long == name } ?: error("Can't find $name value")
        if(!field.needsArgument) error("$name is a flag")
        arguments[field] = value
    }
    private var freeStr: List<String>? = null
    fun free(vararg strings: String) {
        if(freeStr != null) error("Can't specify free strings several times")
        freeStr = strings.toList()
    }

}

private fun String.splitParse() = this.split(Regex("""\s+""")).filter { it.isNotEmpty() }

private fun parse(str: String) = parser.parse(str.splitParse().toTypedArray())

private fun <V,E> PossibleValue<V,E>.assertNotFailure() = ifFailReturn {
    fail("Should not have failed ${getFailUnsafe()}")
}
private fun <V,E> PossibleValue<V,E>.assertFailure() = ifSuccessReturn {
    fail("Should have failed, got ${getSuccessUnsafe()} instead")
}
