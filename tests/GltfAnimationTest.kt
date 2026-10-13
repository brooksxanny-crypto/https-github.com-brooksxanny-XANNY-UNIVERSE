import universe.xanny.app.render.*
import kotlin.math.abs
fun near(a:Float,b:Float)=abs(a-b)<.0001f
fun main(){
    val move=AnimationChannel(0,"translation","LINEAR",floatArrayOf(0f,2f),floatArrayOf(0f,0f,0f,2f,0f,0f))
    check(near(move.sample(1f)[0],1f))
    val step=AnimationChannel(0,"scale","STEP",floatArrayOf(0f,1f),floatArrayOf(1f,1f,1f,2f,2f,2f))
    check(near(step.sample(.5f)[0],1f))
    val rotate=AnimationChannel(0,"rotation","LINEAR",floatArrayOf(0f,1f),floatArrayOf(0f,0f,0f,1f,0f,0f,1f,0f))
    val q=rotate.sample(.5f);check(near(q[2],.70710677f)&&near(q[3],.70710677f))
    val rest=listOf(JointTRS(),JointTRS())
    val animation=ImportedAnimation("move",listOf(move))
    val evaluated=animation.evaluate(1f,rest)
    check(near(evaluated[0].translation.x,1f)&&near(evaluated[1].translation.x,0f))
    val skin=ImportedSkin(listOf(1),listOf(Mat4.IDENTITY),listOf(null))
    val world=AnimatedSkinPose.jointMatrices(skin,intArrayOf(-1,0),listOf(JointTRS(Vec3(1f,0f,0f)),JointTRS(Vec3(0f,2f,0f))))
    check(near(world[0].transformPoint(Vec3(0f,0f,0f)).x,1f))
    check(near(world[0].transformPoint(Vec3(0f,0f,0f)).y,2f))
    var rejected=false
    try { AnimationChannel(0,"translation","LINEAR",floatArrayOf(1f,1f),floatArrayOf(0f,0f,0f,1f,0f,0f)) }catch(_:IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS animation sampler and hierarchical skin pose")
}
