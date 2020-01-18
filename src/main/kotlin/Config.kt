package dev.mee42

import java.io.File

object Config {
    private const val ENV_FLAG = "ADN_CONFIG"
    private val DEFAULT_FILE = "${System.getProperty("user.home")}/.adn/config"
    init {
        verboseOut("Default file: $DEFAULT_FILE")
    }
    private var map :Map<String,InputServer> = emptyMap()
    fun init() {
        val file = File(System.getProperty(ENV_FLAG) ?: DEFAULT_FILE)
        verboseOut("Setting map using config file $file")
        if(!file.exists()) {
            verboseOut("File does not exist - creating")
            file.parentFile.mkdirs()
            file.writeText("""
                # this is the aliases file
                # for documentation, run `man adn`, and find the section on aliases
                # note: aliases are not recursive
                # this is the server that's used when no server is specified
                default:${DEFAULT_DEFAULT_SERVER.asString()}
                # this aliases localhost to localhost with a path. 
                localhost:localhost(${LOCALHOST_DEFAULT_DEFAULT_PATH}
            """.trimIndent())
        }
        map = file.readLines()
            .map { line -> if(line.contains('#')) line.substring(0,line.indexOf('#')) else line }
            .filter { line -> line.isNotBlank() }
            .map { line -> line.trim() }
            .map { line ->
                if(!line.contains(':')) {
                    crashAndExit("Line `$line` in $file doesn't contain a `:`")
                }
                if(line.count { it == ':' } > 1) {
                    crashAndExit("Line `$line` in $file contains too many semicolons")
                }
                val alias = line.split(':')[0]
                val serverString = line.split(':')[1]
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

    val LOCALHOST_DEFAULT_DEFAULT_PATH = "${System.getProperty("user.home")}/.adn/data"
    private val DEFAULT_DEFAULT_SERVER = LocalhostServer(LOCALHOST_DEFAULT_DEFAULT_PATH)

}