package dev.mee42

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result;
import com.github.kittinunf.result.getAs
import java.io.File

typealias ID = String


interface Server {
    fun asString(): String
}

interface InputServer :Server {
    fun toOutput(): OutputServer
    fun post(data: ByteArray, suggestedID: ID?): ID
}
interface OutputServer: Server {
    fun get(id: ID): ByteArray?
}

fun ID.toFileName(): String{
    return "$this.bin"
}
class LocalhostServer(val path: String): InputServer, OutputServer {
    private fun String.fixTilda():String {
        return if(!trimStart().startsWith("~"))
            trimStart()
        else
            System.getProperty("user.home") + trimStart().substring(1)
    }

    override fun toOutput() = this
    private val parent = File(path.fixTilda())
    override fun post(data: ByteArray, suggestedID: ID?): ID {
        val isDuplicate = { it: ID -> File(parent,it.toFileName()).exists() }
        val id = suggestedID?.takeUnless { isDuplicate(it) } ?: generateID(isDuplicate)
        parent.mkdirs()
        val actualFile = File(parent, id.toFileName())
        actualFile.createNewFile()
        actualFile.writeBytes(data)
        return id
    }

    override fun get(id: ID): ByteArray? {
        val file = File(parent, id.toFileName())
        if(!file.exists()) return null
        return file.readBytes()
    }

    override fun asString() = "localhost($path)"

}
open class ExternalServer(val url: String): OutputServer {
    override fun get(id: ID): ByteArray? {
        if(!url.startsWith("https://") && !url.startsWith("http://")){
            crashAndExit("url \"$url\" needs to start with http:// or https://",2)
        }
        return ("$url/raw/$id").httpGet()
            .response().let { (_,_,result) ->
                when(result){
                    is Result.Failure<*> -> throw result.error
                    is Result.Success<*> -> result.get()
                }
            }
    }

    override fun asString() = url
}

fun <A,B> A.applyOperandOptional(operand: B?, block: (A,B) -> A) = if(operand == null) this else block(this,operand)

class ExternalInputServer(val param: String?, url: String): ExternalServer(url), InputServer {
    override fun toOutput(): OutputServer = this
    override fun post(data: ByteArray, suggestedID: ID?): ID {
        val uri = "https://$url/submit" + (param?.let { "/$it" }?:"")
        verboseOut("Connecting to $uri")
        return uri.httpPost()
            .body(data)
            .applyOperandOptional(suggestedID) { request, s -> request.header("name" to s) }
            .responseString().let { (_,_,result) ->
                when(result){
                    is Result.Failure<*> -> throw result.error
                    is Result.Success<*> -> result.get()
                }
            }// ?: crashAndExit("Didn't get string result back from server", 3)
    }

    override fun asString() = url + param?.let { "($it)" }.orEmpty()
}
