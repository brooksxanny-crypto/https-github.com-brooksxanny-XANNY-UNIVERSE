import universe.xanny.app.render.*
import kotlin.math.PI
import kotlin.math.abs
fun sceneClose(a:Float,b:Float)=abs(a-b)<0.0001f
fun main() {
    val graph=SceneGraph(listOf(
        SceneNode("root",null,Vec3(1f,2f,3f),2f,"moonlight",(PI/2).toFloat()),
        SceneNode("child","root",Vec3(1f,0f,1f),.5f,"warm",0f),
        SceneNode("grand","child",Vec3(1f,0f,0f),.5f,"cobalt",0f)
    ),PreviewMaterials.specs)
    val child=graph.resolve("child")
    check(sceneClose(child.worldPosition.x,1f) && sceneClose(child.worldPosition.y,4f) && sceneClose(child.worldPosition.z,5f))
    check(sceneClose(child.worldScale,1f))
    val grand=graph.resolve("grand")
    check(sceneClose(grand.worldPosition.x,1f) && sceneClose(grand.worldPosition.y,5f))
    check(sceneClose(grand.worldScale,.5f))
    var rejected=false
    try { SceneGraph(listOf(SceneNode("x","x",Vec3(0f,0f,0f),1f,"warm")),PreviewMaterials.specs) } catch (_: IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS: inherited rotation, scale, translation and cycle rejection")
}
