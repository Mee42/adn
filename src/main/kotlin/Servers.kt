package dev.mee42

import java.io.File

typealias ID = String


interface Server {
    fun asString(): String
}

interface InputServer :Server {
    fun toOutput(): OutputServer
    fun post(data: ByteArray): ID
}
interface OutputServer: Server {
    fun get(id: ID): ByteArray?
}

class LocalhostServer(val path: String): InputServer, OutputServer {
    private fun String.fixTilda():String {
        return if(!trimStart().startsWith("~"))
            trimStart()
        else
            System.getProperty("user.home") + trimStart().substring(1)
    }
    private fun ID.toFileName(): String{
        return "$this.bin"
    }
    override fun toOutput() = this
    private val parent = File(path.fixTilda())
    override fun post(data: ByteArray): ID {
        val id = generateID { File(parent,it.toFileName()).exists() }
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
        crashAndExit("external servers are not yet supported", 4)
    }

    override fun asString() = url
}
class ExternalInputServer(val param: String?, url: String): ExternalServer(url), InputServer {
    override fun toOutput(): OutputServer = this
    override fun post(data: ByteArray): ID {
        crashAndExit("external servers not yet supported", 4)
    }

    override fun asString() = url + param?.let { "($it)" }.orEmpty()
}
