import universe.xanny.app.render.*
import java.io.File
fun main(args:Array<String>) {
 val bytes=File(args.single()).readBytes()
 val meshes=GltfMeshDecoder.decodeAll(bytes)
 check(meshes.size==2)
 meshes.forEach { m ->
  check(m.normals?.size==9)
  check(m.normals!![2]==1f)
  check(m.material.baseColor.contentEquals(floatArrayOf(.25f,.5f,.75f,1f)))
  check(m.material.metallic==.9f && m.material.roughness==.2f)
 }
 val bad=bytes.copyOf();bad[0]=0
 check(runCatching { GltfMeshDecoder.decodeAll(bad) }.isFailure)
 println("PASS: two primitives, normal attributes, bounded PBR factor metadata, corrupt input rejection")
}
