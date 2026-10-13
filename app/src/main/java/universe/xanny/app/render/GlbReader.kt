package universe.xanny.app.render

import java.security.MessageDigest

/** Validated GLB 2.0 container structure; JSON payload is not yet glTF schema-parsed or rendered. */
data class GlbContainer(val jsonUtf8: String, val binaryChunk: ByteArray?)
object GlbReader {
    private const val MAX_BYTES=32*1024*1024
    private fun u32(b:ByteArray,o:Int):Long {
        require(o>=0 && o+4<=b.size)
        return ((b[o].toLong() and 255) or ((b[o+1].toLong() and 255) shl 8) or
            ((b[o+2].toLong() and 255) shl 16) or ((b[o+3].toLong() and 255) shl 24))
    }
    fun verifyAsset(bytes:ByteArray, descriptor:AssetDescriptor):GlbContainer {
        require(descriptor.type==AssetType.GLB && bytes.size.toLong()==descriptor.byteLength)
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        require(digest.equals(descriptor.sha256,true)) { "SHA256 mismatch" }
        return read(bytes)
    }
    fun read(bytes:ByteArray):GlbContainer {
        require(bytes.size in 20..MAX_BYTES)
        require(u32(bytes,0)==0x46546c67L) { "Not a GLB file" }
        require(u32(bytes,4)==2L) { "Unsupported GLB version" }
        require(u32(bytes,8)==bytes.size.toLong()) { "Invalid declared length" }
        var cursor=12
        var json:String?=null
        var bin:ByteArray?=null
        var part=0
        while(cursor<bytes.size) {
            require(cursor+8<=bytes.size)
            val length=u32(bytes,cursor)
            val type=u32(bytes,cursor+4)
            require(length%4==0L && length <= bytes.size.toLong()-cursor-8) { "Malformed chunk length" }
            val end=cursor+8+length.toInt()
            when(type) {
                0x4e4f534aL -> {
                    require(part==0 && json==null) { "JSON must be the first chunk" }
                    json=bytes.copyOfRange(cursor+8,end).toString(Charsets.UTF_8).trimEnd(' ','\u0000','\n','\r','\t')
                    require(json.startsWith("{") && json.endsWith("}")) { "Invalid JSON chunk boundary" }
                }
                0x004e4942L -> {
                    require(json!=null && bin==null) { "Unexpected BIN chunk" }
                    bin=bytes.copyOfRange(cursor+8,end)
                }
                else -> throw IllegalArgumentException("Unsupported GLB chunk type")
            }
            cursor=end; part++
        }
        require(json!=null && part in 1..2 && cursor==bytes.size)
        return GlbContainer(json,bin)
    }
}
