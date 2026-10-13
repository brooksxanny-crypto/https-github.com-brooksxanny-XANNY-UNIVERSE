package universe.xanny.app.render

import kotlin.math.min

/** Reconstruct view-space depth from normalized perspective depth, then ray distance. */
object DepthRayLimit {
    fun distance(depth: Float, near: Float, far: Float, cosForward: Float, maxDistance: Float=13f): Float {
        require(depth.isFinite() && depth in 0f..1f)
        require(near.isFinite() && far.isFinite() && near > 0f && far > near)
        require(cosForward.isFinite() && cosForward > 0f && cosForward <= 1f)
        require(maxDistance.isFinite() && maxDistance > 0f)
        val ndc = depth * 2f - 1f
        val z = (2f * near * far) / (far + near - ndc * (far-near))
        return min(maxDistance,z/cosForward)
    }
}
