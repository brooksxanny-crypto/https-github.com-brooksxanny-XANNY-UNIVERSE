package universe.xanny.app.render

/** Validated metadata-level skeleton model, not a rendered/skinned character. */
data class JointDefinition(val id: String, val parentIndex: Int?, val inverseBind: List<Float>)
class SkeletonDefinition(val joints: List<JointDefinition>) {
    init {
        require(joints.isNotEmpty() && joints.size <= 256)
        require(joints.map { it.id }.distinct().size == joints.size)
        joints.forEachIndexed { i,j ->
            require(j.id.isNotBlank() && j.inverseBind.size==16 && j.inverseBind.all { it.isFinite() })
            require(j.parentIndex==null || j.parentIndex in 0 until i) { "Joints must be ordered parent-first" }
        }
    }
    val rootJointIndices: List<Int> get() = joints.indices.filter { joints[it].parentIndex==null }
}
