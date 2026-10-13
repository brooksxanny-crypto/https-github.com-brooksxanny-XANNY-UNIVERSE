package universe.xanny.app.render

/** The same live scene poses are consumed for rendering and volumetric light blockers.
 * Supports precisely the three spherical PreviewScene bodies, not arbitrary models. */
object SceneShadowSync {
    const val MAX_OCCLUDERS = 3
    fun fromPoses(poses: List<BodyPose>): List<ShadowSphere> {
        require(poses.size <= MAX_OCCLUDERS) { "Preview shadow capacity exceeded" }
        require(poses.map { it.body.id }.distinct().size == poses.size)
        return poses.map { pose ->
            ShadowSphere(pose.location.x, pose.location.y, pose.location.z, pose.body.radius)
        }
    }
    fun at(timeSeconds: Float): List<ShadowSphere> {
        require(timeSeconds.isFinite())
        return fromPoses(PreviewScene.poses(timeSeconds))
    }
}
