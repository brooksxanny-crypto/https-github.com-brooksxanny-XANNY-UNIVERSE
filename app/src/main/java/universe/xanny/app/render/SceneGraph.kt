package universe.xanny.app.render

import kotlin.math.cos
import kotlin.math.sin

/** V5 supports hierarchical translation, Z-axis rotation and uniform scale. */
data class SceneNode(
    val id: String, val parentId: String?, val localPosition: Vec3,
    val scale: Float, val materialId: String, val rotationZ: Float = 0f
)
data class WorldNode(
    val node: SceneNode, val worldPosition: Vec3,
    val worldScale: Float, val worldRotationZ: Float
)
data class MaterialSpec(
    val id: String, val baseColor: Vec3, val ambient: Float,
    val diffuse: Float, val specular: Float, val shininess: Float
) {
    init {
        require(id.isNotBlank() && ambient in 0f..1f && diffuse in 0f..1f &&
            specular in 0f..1f && shininess >= 1f)
    }
}

class SceneGraph(nodes: List<SceneNode>, materials: List<MaterialSpec>) {
    private val objects = nodes.associateBy { it.id }
    val materialsById = materials.associateBy { it.id }
    init {
        require(objects.size == nodes.size && nodes.all { it.id.isNotBlank() && it.scale.isFinite() && it.scale > 0f && it.rotationZ.isFinite() && it.materialId in materialsById })
        require(materialsById.size == materials.size)
        nodes.forEach { require(it.parentId == null || it.parentId in objects) }
        nodes.forEach { resolve(it.id) }
    }
    fun resolve(id: String): WorldNode = resolveInternal(id, mutableSetOf())
    private fun resolveInternal(id: String, ancestors: MutableSet<String>): WorldNode {
        val node = objects[id] ?: error("Unknown node: $id")
        require(ancestors.add(id)) { "Scene hierarchy cycle at $id" }
        val parent = node.parentId?.let { resolveInternal(it, ancestors) }
        ancestors.remove(id)
        if (parent == null) return WorldNode(node, node.localPosition, node.scale, node.rotationZ)
        val angle = parent.worldRotationZ.toDouble()
        val c = cos(angle).toFloat(); val s = sin(angle).toFloat()
        val x = node.localPosition.x * parent.worldScale
        val y = node.localPosition.y * parent.worldScale
        val z = node.localPosition.z * parent.worldScale
        return WorldNode(node, Vec3(parent.worldPosition.x + c*x-s*y,
            parent.worldPosition.y+s*x+c*y, parent.worldPosition.z+z),
            parent.worldScale*node.scale, parent.worldRotationZ+node.rotationZ)
    }
}

object PreviewMaterials {
    val specs = listOf(
        MaterialSpec("moonlight", Vec3(.78f,.83f,.92f), .18f, .75f, .22f, 24f),
        MaterialSpec("cobalt", Vec3(.55f,.67f,.92f), .18f, .75f, .20f, 24f),
        MaterialSpec("warm", Vec3(.95f,.82f,.72f), .18f, .75f, .18f, 24f)
    )
}
