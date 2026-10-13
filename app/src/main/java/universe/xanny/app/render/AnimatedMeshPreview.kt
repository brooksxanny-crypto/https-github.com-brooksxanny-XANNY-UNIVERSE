package universe.xanny.app.render

import kotlin.math.sin

/**
 * Proof-of-pipeline for a supported GLB asset.
 * Uses imported JOINTS_0/WEIGHTS_0 and inverse binds, with explicitly authored
 * periodic joint translation; NOT glTF animation playback or a production avatar.
 */
class AnimatedMeshPreview(private val mesh: ImportedTriangleMesh, private val skin: ImportedSkin) {
    init { require(mesh.skinWeights != null && skin.nodeIndices.size >= 2) }
    fun positions(timeSeconds: Float): FloatArray {
        require(timeSeconds.isFinite())
        val movement = sin(timeSeconds.toDouble()).toFloat() * .5f
        val transforms = List(skin.nodeIndices.size) { i ->
            if (i == 1) Mat4.translation(movement, 0f, 0f) else Mat4.IDENTITY
        }
        return skin.deform(mesh, transforms)
    }
}
