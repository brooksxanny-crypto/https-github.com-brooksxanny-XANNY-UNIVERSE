import universe.xanny.app.render.*
import java.io.File
import kotlin.math.abs
fun main(args:Array<String>) {
 val bytes=File(args[0]).readBytes()
 val mesh=GltfMeshDecoder.decode(bytes)
 val skin=GltfMeshDecoder.decodeSkin(bytes)
 check(mesh.skinWeights!=null && skin.nodeIndices==listOf(0,1))
 check(skin.parentJointIndices==listOf(null,0))
 val moved=skin.deform(mesh,listOf(Mat4.IDENTITY,Mat4.translation(2f,0f,0f)))
 check(abs(moved[0]-1f)<.0001f && abs(moved[3]-2f)<.0001f)
 var rejected=false
 val invalid=bytes.copyOf()
 // Wrong skin weight sum by replacing a float in binary payload using known raw byte patterns.
 val pattern=java.nio.ByteBuffer.allocate(16).order(java.nio.ByteOrder.LITTLE_ENDIAN)
   .putFloat(.5f).putFloat(.5f).putFloat(0f).putFloat(0f).array()
 val offset=invalid.indices.firstOrNull { k -> k+pattern.size<=invalid.size && invalid.copyOfRange(k,k+pattern.size).contentEquals(pattern) }!!
 for (b in 0..3) invalid[offset+b]=0
 try { GltfMeshDecoder.decode(invalid) } catch(_:IllegalArgumentException) { rejected=true }
 check(rejected)
 println("PASS skin decode, joint hierarchy, inverse binds, CPU deformation and invalid weights")
}
