package dev.mee42

import java.lang.RuntimeException
import kotlin.system.exitProcess


val parser = ArgumentParser.of {
    flag(short = 'h', long = "help")
    flag(long = "version")
    flag(short = 'v', long = "verbose")
    flag(long = "license")
    flag(short = 'i', long = "in")
    flag(short = 'o', long = "out")

    arg(short = 's', long = "server")
    arg(short = 'O', long = "output-format")
    flag(short = 'u', long = "url")

    arg(short = 'n', long = "name")

    flag(long = "run-server")
    arg(long = "port")
}

enum class OutputFormat(val names: List<String>, val desc: String) {
    INPUT(listOf("INPUT"),"exactly what you put in"),
    OUTPUT(listOf("OUTPUT"),"strips external server parameters, perfect for pipes and such"),
    VERBOSE(listOf("VERBOSE"), "prints all information"),
    URL(listOf("URL"), "in a clickable format - only for external servers");
    companion object {
        fun getForString(str: String) : OutputFormat {
            return values().firstOrNull { it.names.contains(str.toUpperCase()) }
                ?: crashAndExit("Can't get output format \"$str\"")
        }
    }
}


var verbose = false
lateinit var parsedArguments :ParsedArguments
fun crashAndExit(message: String, code: Int = -1): Nothing {
    System.err.println("\n$message")
    if(verbose) {
        RuntimeException("" + code).printStackTrace()
    }
    exitProcess(code)
}
fun verboseOut(str: Any) {
    if(verbose) println(str)
}


fun main(args: Array<String>) {
    try {
        parsedArguments = parser.parse(args).ifFailReturn {
            System.err.println(it.message)
            return
        }
        verbose = parsedArguments.isFlagSpecified("verbose")
        verboseOut("Called: ${args.toList()}")
        if (parsedArguments.isFlagSpecified("help")) {
            println(HELP_MENU)
            return
        }
        if (parsedArguments.isFlagSpecified("version")) {
            println("Version: $VERSION")
            return
        }
        if (parsedArguments.isFlagSpecified("license")) {
            println(LICENSE)
            return
        }
        if(parsedArguments.isFlagSpecified("run-server")) {
            serverMode()
        }
        Config.init()

        val `in` = parsedArguments.isFlagSpecified("in")
        val out = parsedArguments.isFlagSpecified("out")
        if (`in` && out) crashAndExit("Can't go both in and out", 2)
        when {
            `in` -> input()
            out -> output()
            parsedArguments.free.isNotEmpty() -> {
                verboseOut("Assuming it's output. Free" +
                        parsedArguments.free.fold("["){it, acc -> "$it,$acc" } + "]")
                output()
            }
            else -> {
                // if there's nothing, it's assumed to be input
                verboseOut("Assuming it's input")
                input()
            }
        }
    } catch (e: Throwable) {
        System.err.println("Error: ${e.message}")
        if(verbose) e.printStackTrace()
        exitProcess(3)
    }
}

/**
 * Preconditions: serverString matches spec, including not having any form of ID.
 * */
fun parseInputServer(serverString: String) :InputServer {
    verboseOut("parsing input server string \"$serverString\"")
    if(serverString.isBlank()) {
        // no param specifiedCan't find the address in the arguments

        return Config.getDefaultInputServer()
    }
    if(serverString.contains(' ')) crashAndExit("server string \"$serverString\" can't contain a space", 2)

    val (name, param) = if(serverString.contains("(")) {
        // it has a param, it's let's extract that
        val param = serverString.substring(serverString.indexOf('(') + 1).dropLast(1)
        val name = serverString.substring(0, serverString.indexOf('('))// TODO check for spaces
        (name to param)
    } else {
        serverString to null
    }
    when (val alias = Config.getServerForAlias(name)) {
            null -> {}
            is LocalhostServer -> return LocalhostServer(param ?: alias.path)
            is ExternalInputServer -> return ExternalInputServer(url = alias.url, param = param ?: alias.param)
            else -> crashAndExit("alias is not a known type ", 5)
    }
    return if(name == "localhost") {
        LocalhostServer(param ?: Config.LOCALHOST_DEFAULT_DEFAULT_PATH)
    } else {
        if(!serverString.contains('.')){
            if(verbose) Config.printAliases()
            crashAndExit("Can't use `$serverString` - can't find any alias and it's not a url", 2)
        }
        ExternalInputServer(url = serverString, param = param)
    }
}
fun parseOutputServer(serverString: String): Pair<OutputServer,String> {
    verboseOut("parsing output server string \"$serverString\"")

    // this has both the ID and the server, so first strip out the ID
    if(serverString.count { it == ':'} > 1) crashAndExit("The server string can only have one ':'", 2)
    if((serverString.startsWith(':') && serverString.count { it == ':' } == 1)
        || !serverString.contains(':')) {
        // okay just use the default server
        // unless... there's a server in the parameter
        val serverToUse = parsedArguments.getArg("server")
            ?.let { Config.getServerForAlias(it)?.toOutput() }
            ?: Config.getDefaultInputServer().toOutput()
        return serverToUse to serverString
    }
    val id = serverString.split(':')[1]
    val server = parseInputServer(serverString.split(':')[0])
    if(server is ExternalInputServer && server.param != null) crashAndExit("Output server can not have parameter", 2)
    return server.toOutput() to id
}


fun input(){
    val server = parseInputServer(parsedArguments.getArg("server") ?: "")
    verboseOut("using server ${server.asString()}")

    val data = System.`in`.readAllBytes()
    val id = server.post(data, parsedArguments.getArg("name"))
    val  outputFormat = parsedArguments.isFlagSpecified("url").takeIf { it }?.let { OutputFormat.URL }
                    ?: parsedArguments.getArg("output-format")?.let { OutputFormat.getForString(it) }
                    ?: OutputFormat.OUTPUT
    val strOut = when (outputFormat) {
        OutputFormat.INPUT -> parsedArguments.getArg("server")?.let { "$it:" }.orEmpty() + id
        OutputFormat.OUTPUT -> parsedArguments.getArg("server")?.let {
            if(it.contains('(') && server !is LocalhostServer)
                it.substring(0,it.indexOf('(')) + ":"
            else "$it:"
        }.orEmpty() + id
        OutputFormat.VERBOSE -> server.asString() + ":" + id
        OutputFormat.URL -> {
            when (server) {
                is LocalhostServer -> crashAndExit("Can't format localhost as a url", 2)
                is ExternalInputServer -> "${server.url}/$id"
                else -> crashAndExit("I can't format server \"${server.asString()}\" of type ${server.javaClass.name}", 5)
            }
        }
    }
    println(strOut)
}
fun output(){
    val input = parsedArguments.free.firstOrNull()
        ?: System.`in`.readAllBytes().takeUnless { it.isEmpty() }?.map { it.toChar() }?.fold("",{a,b -> a + b})?.trim()
        ?: crashAndExit("Can't find the address in the arguments", 2)
    val (server, id) = parseOutputServer(input)
    verboseOut("Using server `${server.asString()}` ID:$id")
    val bytes = server.get(id = id) ?: crashAndExit("Document does not appear to exist",1)
    System.out.write(bytes)
}
