import universe.xanny.app.render.*
import kotlin.math.abs

fun main() {
    val bodies=PreviewScene.bodies
    check(bodies.size == 3)
    check(bodies.map { it.id }.toSet().size == 3)
    val initial=PreviewScene.poses(0f)
    val later=PreviewScene.poses(3f)
    check(initial.first().location == later.first().location)
    check(initial[1].location != later[1].location)
    val clamped=OrbitCamera().adjusted(0f,100f,-100f)
    check(clamped.elevation <= 1.35f && clamped.distance == 4.5f)
    val far=OrbitCamera().adjusted(0f,-100f,100f)
    check(far.elevation >= -1.35f && far.distance == 18f)
    val p=OrbitCamera().position()
    check(abs(p.z)>0f)
    println("PASS: three deterministic orbital bodies, unique IDs, bounded camera")
}
