package dev.mee42

import java.io.File

object Config {

    val LOCALHOST_DEFAULT_DEFAULT_PATH = "${System.getProperty("user.home")}/.adn/data"
    private val DEFAULT_DEFAULT_SERVER = LocalhostServer(LOCALHOST_DEFAULT_DEFAULT_PATH)
    private const val ENV_FLAG = "ADN_CONFIG"
    private val DEFAULT_CONFIG_FILE_PATH = "${System.getProperty("user.home")}/.adn/config"
    private val DEFAULT_CONFIG_FILE = """
# this is the aliases file
# for documentation, run `man adn`, and find the section on aliases

# note: aliases are not recursive and are evaluated with no aliases present

# this is the server that's used when no server is specified. You may want to switch this to a web server
default:${DEFAULT_DEFAULT_SERVER.asString()}

# when installed, there is a adnw executable as well, which calls adn with `-s web` and any additional arguments
web:https://adn.mee42.dev

# this aliases localhost to localhost with a path.
localhost:localhost($LOCALHOST_DEFAULT_DEFAULT_PATH)

# "here" is the current directory. You may want to have `./.adn/` instead
here:localhost(./adn/)

# temp is often deleted, so don't expect it to stay around long
temp:localhost(/tmp/adn/)
tmp:localhost(/tmp/adn)

# cache goes in ~/.cache, and is good for temporary big files (that won't fit into ram)
cache:localhost(~/.cache/adn/)

# A public server (the only one as of right now)
mee42.dev:https://adn.mee42.dev
mee42:https://adn.mee42.dev
    """.trimIndent()

    init {
        verboseOut("Default file: $DEFAULT_CONFIG_FILE_PATH")
    }
    private var map :Map<String,InputServer> = emptyMap()
    fun init() {
        val file = File(System.getProperty(ENV_FLAG) ?: DEFAULT_CONFIG_FILE_PATH)
        verboseOut("Setting map using config file $file")
        if(!file.exists()) {
            verboseOut("File does not exist - creating")
            file.parentFile.mkdirs() // TODO update the default file
            file.writeText(DEFAULT_CONFIG_FILE)
        }
        map = file.readLines()
            .map { line -> if(line.contains('#')) line.substring(0,line.indexOf('#')) else line }
            .filter { line -> line.isNotBlank() }
            .map { line -> line.trim() }
            .map { line ->
                if(!line.contains(':')) {
                    crashAndExit("Line `$line` in $file doesn't contain a `:`")
                }
                val (alias, serverString) = line.split(':',limit = 2)
                alias to parseInputServer(serverString)
            }.toMap()
    }

    fun getServerForAlias(alias: String): InputServer? {
        return map[alias]
    }
    fun getDefaultInputServer(): InputServer {
        return getServerForAlias("default") ?: DEFAULT_DEFAULT_SERVER
    }

    fun printAliases() {
        println(">> aliases")
        for((key,value) in map) {
            println(key + ":" + value.asString())
        }
        println("<<")
    }


}