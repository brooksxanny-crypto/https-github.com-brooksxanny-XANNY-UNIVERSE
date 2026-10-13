import universe.xanny.app.render.*
import kotlin.math.abs
fun main() {
    val a=SceneShadowSync.at(0f);val b=SceneShadowSync.at(4f)
    check(a.size == PreviewScene.bodies.size && a.size == 3)
    check(a[0].radius == PreviewScene.bodies[0].radius)
    check(a[0] == b[0])
    check(abs(a[1].x-b[1].x)>0.01f || abs(a[1].z-b[1].z)>0.01f)
    val poses=PreviewScene.poses(4f)
    check(b.indices.all { i -> b[i].x==poses[i].location.x && b[i].radius==poses[i].body.radius })
    check(VolumeSphereOcclusion.blocksLight(0f,0f,-3f,0f,0f,1f,4f,b[0]))
    try { SceneShadowSync.at(Float.NaN); error("expected reject") } catch(_:IllegalArgumentException) {}
    check("uShadowSpheres[3]" in VolumetricRaymarch.fragmentShader)
    check("sceneBlocksLight" in VolumetricRaymarch.fragmentShader)
    println("PASS V22 scene-animated spherical blocker sync")
}
