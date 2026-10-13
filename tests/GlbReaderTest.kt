import universe.xanny.app.render.*
import java.security.MessageDigest
fun le(v:Int)=byteArrayOf(v.toByte(),(v ushr 8).toByte(),(v ushr 16).toByte(),(v ushr 24).toByte())
fun main(){
    val body="{\"asset\":{\"version\":\"2.0\"}}".toByteArray()
    val padded=body+ByteArray((4-body.size%4)%4){32}
    val bytes=le(0x46546c67)+le(2)+le(20+padded.size)+le(padded.size)+le(0x4e4f534a)+padded
    val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
    val descriptor=AssetDescriptor("sample","objects/sample.glb",hash,AssetType.GLB,bytes.size.toLong())
    check(GlbReader.verifyAsset(bytes,descriptor).jsonUtf8.contains("2.0"))
    var rejected=false
    try { GlbReader.verifyAsset(bytes.copyOf(bytes.size-1),descriptor) } catch(e:IllegalArgumentException){rejected=true}
    check(rejected)
    rejected=false
    try {GlbReader.read(bytes.copyOf().apply{this[4]=1})} catch(e:IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS: GLB binary container, digest, tamper and version validation")
}
