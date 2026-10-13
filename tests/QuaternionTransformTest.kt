import universe.xanny.app.render.*
import kotlin.math.PI
import kotlin.math.abs
fun eq(a:Float,b:Float)=abs(a-b)<0.0002f
fun main() {
    val q=Quaternion.axisAngle(Vec3(0f,1f,0f),PI.toFloat()/2)
    val r=q.rotate(Vec3(1f,0f,0f))
    check(eq(r.x,0f)&&eq(r.z,-1f))
    val root=Transform3D(Vec3(1f,2f,3f),q,2f)
    val child=Transform3D(Vec3(1f,0f,0f),Quaternion.IDENTITY,.5f)
    val world=root.child(child)
    check(eq(world.translation.x,1f)&&eq(world.translation.z,1f)&&eq(world.scale,1f))
    val tree=TransformHierarchy(mapOf("root" to (null to root),"child" to ("root" to child)))
    check(eq(tree.resolve("child").translation.z,1f))
    var denied=false
    try { TransformHierarchy(mapOf("a" to ("b" to root),"b" to ("a" to child))) } catch(e:IllegalArgumentException){denied=true}
    check(denied)
    println("PASS: quaternion composition, 3D hierarchy, cycle prevention")
}
