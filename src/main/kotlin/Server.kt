package dev.mee42

import spark.Spark.initExceptionHandler
import spark.kotlin.*
import java.util.regex.Pattern


@Suppress("ControlFlowWithEmptyBody")
fun serverMode(): Nothing {
    // start a simple https server
    verboseOut("Started server")
    val port = parsedArguments.getArg("port")?.toInt() ?: error("No port specified")
    println("Running at port $port")
    port(port)
    initExceptionHandler { it.printStackTrace() }
    inMemoryServer()
    while(true){}
}

fun inMemoryServer(){
    verboseOut("Started in memory server")
    val data = mutableMapOf<ID,ByteArray>()
    get("/") {
        verboseOut("hit /")
        """
            |simple adn server: <a href="https://adn.mee42.dev">adn.mee42.dev</a>
            |
            |<p>A content delivery network, but only for textual content</p>
            |
        """.trimMargin()
    }
    get("/:id") {
        val initId = this.request.params("id")
        val (id, lang) = if(initId.contains(".")) {
            val array = initId.split(".",limit = 2)
            array[0] to array[1]
        } else { initId to null }
        val stringyData = String(data[id] ?: throw halt(200,"""
            <!DOCTYPE html>
            <html>
            <head>
              <meta content="text/html;charset=utf-8" http-equiv="Content-Type">
              <meta name="description" content="404 not found">
              <title>adn.mee42.dev:$id</title>
            </head>
            <body>
            <h1> 404 not found </h1>
            </body>
            </html>
        """.trimIndent()),Charsets.UTF_8)
        """
            <!DOCTYPE html>
            <html>
            <head>
              <meta content="text/html;charset=utf-8" http-equiv="Content-Type">
              <meta name="description" content="${
        if(stringyData.length > 500) stringyData.substring(0,500) else stringyData
        }">
              <title>adn.mee42.dev:$id</title>
              <link rel="stylesheet" href="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.17.1/build/styles/default.min.css">
              <script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.17.1/build/highlight.min.js"></script>
              <script>hljs.initHighlightingOnLoad();</script>
              <style>
              body, html, pre, code {
                  margin: 0;
                  height: 100%;
                  width: 100%;
              }
              </style>
            </head>
            <body>
            <pre><code ${
            lang?.let { if(it == "txt") "class=\"plaintext\"" else "class=\"lang-${it}\"" } ?: ""
            }>
$stringyData
            </code></pre>
            </body>
            </html>
        """.trimIndent()

    }
    get("/raw/:id") {
        verboseOut("hit /raw/${this.request.params("id")}")
        val id = this.request.params("id")
        data[id] ?: halt(404, "content not found")
    }

    fun RouteHandler.post(param: String) :ID {
        if(param != "default") halt(405, "only param supported is \"default\"")
        val requestedName = this.request.headers("name")
        requestedName?.let{ verboseOut("name requested: $requestedName") }
        val bytes = this.request.bodyAsBytes()

        if(requestedName == null) {
            // if requestedName doesn't exist, check for existing value, return that if it exists
            val possibleExists = data.toList().firstOrNull {
                it.second.contentEquals(bytes)
            }?.first
            if (possibleExists != null) return possibleExists
        }
        // if requestedName doesn't exist already
        val id = if(requestedName != null && data[requestedName] == null) requestedName else generateID { data[it] != null }

        if(bytes.size > 104857600/*100 MiB*/) halt(406, "content too big")
        data[id] = bytes
        return id
    }

    post("/submit") {
        // no param, just normal
        verboseOut("hit /submit")
        this.post("default")
    }
    post("/submit/:param") {
        val param = this.request.params("param")
        verboseOut("hit /submit/$param")
        this.post(param)
    }

}
