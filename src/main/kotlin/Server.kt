package dev.mee42

import org.eclipse.jetty.http.HttpParser
import spark.Spark.initExceptionHandler
import spark.kotlin.*
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.net.URLConnection
import kotlin.system.exitProcess


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
        val (id, possibleLang ) = if(initId.contains(".")) {
            val array = initId.split(".",limit = 2)
            array[0] to array[1]
        } else { initId to null }
        println("DEBUG: possibleLang = $possibleLang")
        val betterLang = (possibleLang ?: request.queryParams("lang"))
        println("DEBUG: betterLang = $possibleLang")
        val lang = betterLang?.let { langMap[it] ?: it }
        println("hit /$initId with lang = $lang")

        val packet = data[id] ?: throw halt(404, "content not found")
        val type = URLConnection.guessContentTypeFromStream(BufferedInputStream(ByteArrayInputStream(packet)))
        if(type?.contains("image") == true) {
            response.type(type)
            return@get packet
        }
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
        val htmlString = stringyData.replace("&","&amp;").replace("<","&lt;")
        val descString = htmlString.replace("\"","&quot;")
        """
            <!DOCTYPE html>
            <html>
            <head>
              <meta content="text/html;charset=utf-8" http-equiv="Content-Type">
              <meta name="description" content="${
        if(descString.length > 500) descString.substring(0,500) else descString
        }">
              <title>adn.mee42.dev:$id</title>
              <link rel="stylesheet" href="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.17.1/build/styles/default.min.css">
              <script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.17.1/build/highlight.min.js"></script>
              ${lang?.let { "<script charset=\"UTF-8\" src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.15.9/languages/$it.min.js\"></script>" } ?: ""}
              <script src="//cdn.jsdelivr.net/npm/highlightjs-line-numbers.js@2.7.0/dist/highlightjs-line-numbers.min.js"></script>
              <script>hljs.initHighlightingOnLoad(); hljs.initLineNumbersOnLoad();</script>
              <style>
              body, html, pre, code {
                  margin: 0;
                  height: 100%;
                  width: 100%;
              }
              .hljs-ln-n {
                  padding-right: 5px;
              }
              </style>
            </head>
            <body>
            <pre><code ${
            lang?.let { if(it == "txt") "class=\"plaintext\"" else "class=\"lang-${it}\"" } ?: ""
            }>
$htmlString
            </code></pre>
            </body>
            </html>
        """.trimIndent()

    }
    get("/raw/:id") {
        val id = this.request.params("id")
        verboseOut("hit /raw/$id")
        val packet = data[id] ?: throw halt(404, "content not found")
        val type = URLConnection.guessContentTypeFromStream(BufferedInputStream(ByteArrayInputStream(packet)))
        response.type(type)
        return@get packet
    }
    fun RouteHandler.down(filename: String?) : ByteArray{
        val id = this.request.params("id")
        verboseOut("hit /down/$id" + filename?.let { "/$it" }.orEmpty())
        val packet = data[id] ?: throw halt(404, "content not found")
        val file = filename ?: "$id.bin"
        response.header("Content-Disposition", "attachment; filename=$file")
        val type = URLConnection.guessContentTypeFromStream(BufferedInputStream(ByteArrayInputStream(packet)))
        response.type(type)
        return packet
    }
    get("/down/:id/:filename") {
        return@get down(this.request.params("filename"))
    }
    get("/down/:id") {
        return@get down(null)
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

        if(bytes.size > 104857600/*100 MiB*/) halt(413, "content too big")
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