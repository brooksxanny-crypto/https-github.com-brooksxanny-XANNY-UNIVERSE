import universe.xanny.app.render.*
import kotlin.math.abs

fun cpuClose(a:Float,b:Float)=abs(a-b)<.0001f
fun main() {
    val identity=Mat4.IDENTITY
    val translated=Mat4.translation(2f,0f,0f)
    val half=SkinWeights(intArrayOf(0,1,0,0),floatArrayOf(.5f,.5f,0f,0f),1)
    val output=CpuSkinning.deform(floatArrayOf(1f,2f,3f),half,listOf(identity,translated),listOf(identity,identity))
    check(cpuClose(output[0],2f)&&cpuClose(output[1],2f)&&cpuClose(output[2],3f))
    val inverse=CpuSkinning.deform(floatArrayOf(1f,2f,3f),
      SkinWeights(intArrayOf(0,0,0,0),floatArrayOf(1f,0f,0f,0f),1),
      listOf(translated),listOf(Mat4.translation(-2f,0f,0f)))
    check(cpuClose(inverse[0],1f))
    var rejected=false
    try {SkinWeights(intArrayOf(0,0,0,0),floatArrayOf(.3f,.3f,0f,0f),1)}catch(_:IllegalArgumentException){rejected=true}
    check(rejected)
    rejected=false
    try {CpuSkinning.deform(floatArrayOf(0f,0f,0f),
      SkinWeights(intArrayOf(7,0,0,0),floatArrayOf(1f,0f,0f,0f),1),
      listOf(identity),listOf(identity))}catch(_:IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS CPU skinning: linear blend, inverse bind, invalid weights and invalid indices")
}
