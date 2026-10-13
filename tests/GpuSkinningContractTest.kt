import universe.xanny.app.render.*
import java.io.File
import kotlin.math.abs

fun main(args:Array<String>) {
    val bytes=File(args[0]).readBytes()
    val mesh=GltfMeshDecoder.decode(bytes)
    val skin=GltfMeshDecoder.decodeSkin(bytes)
    GpuSkinningContract.validate(mesh,skin)
    val m=GpuSkinningContract.jointMatrices(skin,1.5707964f)
    val cpu=skin.deform(mesh,listOf(Mat4.IDENTITY,Mat4.translation(.5f,0f,0f)))
    val weight=mesh.skinWeights!!
    val expected=FloatArray(mesh.positions.size)
    for(i in 0 until weight.vertexCount) {
        val v=Vec3(mesh.positions[3*i],mesh.positions[3*i+1],mesh.positions[3*i+2])
        var x=0f;var y=0f;var z=0f
        for(k in 0..3) {
            val j=weight.jointIndices[4*i+k]; val w=weight.weights[4*i+k]
            if(w>0f) {
                val t=Mat4(m[j]).transformPoint(v)
                x+=w*t.x;y+=w*t.y;z+=w*t.z
            }
        }
        expected[3*i]=x;expected[3*i+1]=y;expected[3*i+2]=z
    }
    check(cpu.indices.all { abs(cpu[it]-expected[it])<0.0002f })
    check(GpuSkinningContract.vertexShader.contains("aJoints"))
    var bad=false
    try { GpuSkinningContract.jointMatrices(skin,Float.NaN) } catch(_:IllegalArgumentException){bad=true}
    check(bad)
    println("PASS V13 shader contract, 2-joint matrices, CPU-equivalent calculated deformation")
}
