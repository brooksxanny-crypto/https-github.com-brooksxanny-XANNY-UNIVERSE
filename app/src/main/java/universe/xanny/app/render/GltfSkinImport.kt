package universe.xanny.app.render

/** Joint indices follow the original glTF skin.joints order, not hierarchy order. */
data class ImportedSkin(
    val nodeIndices: List<Int>,
    val inverseBindMatrices: List<Mat4>,
    val parentJointIndices: List<Int?>
) {
    init {
        require(nodeIndices.isNotEmpty() && nodeIndices.size<=256)
        require(nodeIndices.distinct().size==nodeIndices.size)
        require(inverseBindMatrices.size==nodeIndices.size && parentJointIndices.size==nodeIndices.size)
        require(parentJointIndices.indices.all { parentJointIndices[it] == null || parentJointIndices[it] in nodeIndices.indices })
    }
    fun deform(mesh: ImportedTriangleMesh, worldMatricesInJointOrder: List<Mat4>): FloatArray {
        val influences=requireNotNull(mesh.skinWeights) { "No skin influences" }
        require(influences.jointIndices.all { it < nodeIndices.size })
        return CpuSkinning.deform(mesh.positions,influences,worldMatricesInJointOrder,inverseBindMatrices)
    }
}
