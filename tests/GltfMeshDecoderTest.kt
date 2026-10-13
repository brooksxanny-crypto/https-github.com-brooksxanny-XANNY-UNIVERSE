import universe.xanny.app.render.*
import java.io.File
fun main(args: Array<String>) {
    val fixture = File(args.single()).readBytes()
    val m = GltfMeshDecoder.decode(fixture)
    check(m.positions.size == 9 && m.indices.size == 3)
    check(m.indices.map { it.toInt() and 65535 } == listOf(0,1,2))
    check(m.positions[0] == -0.7f)
    val broken = fixture.copyOf()
    // Rewrite first index to 90, violating vertex count.
    val where = broken.indexOfSlice(byteArrayOf(0,0,1,0,2,0))
    check(where >= 0)
    broken[where] = 90
    check(runCatching { GltfMeshDecoder.decode(broken) }.isFailure)
    val malformed = fixture.copyOf()
    malformed[0] = 0
    check(runCatching { GltfMeshDecoder.decode(malformed) }.isFailure)
    println("PASS GLB mesh decode and invalid bounds")
}
fun ByteArray.indexOfSlice(pattern:ByteArray):Int {
    for(i in 0..(size-pattern.size)) if(pattern.indices.all { this[i+it] == pattern[it] }) return i
    return -1
}
