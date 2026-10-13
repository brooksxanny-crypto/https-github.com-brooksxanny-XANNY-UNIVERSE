import universe.xanny.app.render.*
fun main() {
    val materials=PreviewMaterials.specs
    val graph=SceneGraph(listOf(
        SceneNode("star",null,Vec3(1f,0f,0f),1f,"moonlight"),
        SceneNode("moon","star",Vec3(2f,0f,0f),.25f,"cobalt")),materials)
    check(graph.resolve("moon").worldPosition.x==3f)
    check(graph.materialsById.size==3)
    var cycleRejected=false
    try { SceneGraph(listOf(SceneNode("a","b",Vec3(0f,0f,0f),1f,"warm"),SceneNode("b","a",Vec3(0f,0f,0f),1f,"warm")),materials) } catch(e: IllegalArgumentException) { cycleRejected=true }
    check(cycleRejected)
    var unknownRejected=false
    try { SceneGraph(listOf(SceneNode("a","lost",Vec3(0f,0f,0f),1f,"warm")),materials) } catch(e: IllegalArgumentException) { unknownRejected=true }
    check(unknownRejected)
    println("PASS: parent transforms, material IDs, cycle and missing-parent rejection")
}
