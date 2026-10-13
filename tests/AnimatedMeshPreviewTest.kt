import universe.xanny.app.render.*
import java.io.File
import kotlin.math.abs

fun main(args: Array<String>) {
    val bytes=File(args[0]).readBytes()
    val mesh=GltfMeshDecoder.decode(bytes)
    val skin=GltfMeshDecoder.decodeSkin(bytes)
    val preview=AnimatedMeshPreview(mesh,skin)
    val atZero=preview.positions(0f)
    val atPeak=preview.positions(1.5707964f)
    check(atZero.size == mesh.positions.size)
    check(atZero.indices.all { abs(atZero[it]-mesh.positions[it]) < .0001f })
    check(atPeak.indices.any { abs(atPeak[it]-atZero[it]) > .01f })
    var rejected=false
    try {preview.positions(Float.NaN)}catch(_:IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS V12 imported GLB CPU-skinned preview deform / reject nonfinite time")
}
