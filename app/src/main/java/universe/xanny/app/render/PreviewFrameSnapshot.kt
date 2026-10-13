package universe.xanny.app.render

/** Immutable per-frame scene sampling. A single animation tick feeds capture,
 * normal scene rendering, and volumetric shadow uniforms.
 * This is only the three preview spheres, not arbitrary scene geometry. */
class PreviewFrameSnapshot private constructor(
    val timeSeconds: Float,
    val poses: List<BodyPose>,
    val shadowSpheres: List<ShadowSphere>
) {
    companion object {
        fun capture(timeSeconds: Float): PreviewFrameSnapshot {
            require(timeSeconds.isFinite() && timeSeconds >= 0f) { "Invalid frame time" }
            val poses=PreviewScene.poses(timeSeconds).toList()
            return PreviewFrameSnapshot(timeSeconds, poses, SceneShadowSync.fromPoses(poses))
        }
    }
}
