package universe.xanny.app.render

import kotlin.math.cos
import kotlin.math.sin

/** Platform-independent authored preview scene; no claims of H3, PBR chrome or rigged avatars. */
data class Vec3(val x: Float, val y: Float, val z: Float)
data class PreviewBody(
    val id: String,
    val radius: Float,
    val orbitRadius: Float,
    val orbitSpeed: Float,
    val phase: Float,
    val rgb: Vec3
)
data class BodyPose(val body: PreviewBody, val location: Vec3)

object PreviewScene {
    val bodies = listOf(
        PreviewBody("primary", 0.95f, 0f, 0f, 0f, Vec3(.78f,.83f,.92f)),
        PreviewBody("satellite_a", .29f, 2.05f, .42f, 0f, Vec3(.55f,.67f,.92f)),
        PreviewBody("satellite_b", .19f, 2.85f, -.28f, 1.5f, Vec3(.95f,.82f,.72f))
    )
    init {
        require(bodies.map { it.id }.distinct().size == bodies.size)
        require(bodies.all { it.radius > 0f && it.orbitRadius >= 0f })
    }
    fun poses(timeSeconds: Float): List<BodyPose> =
        bodies.map { body ->
            val a = timeSeconds * body.orbitSpeed + body.phase
            BodyPose(body, Vec3(cos(a) * body.orbitRadius, 0f, sin(a) * body.orbitRadius))
        }
}

/** Mathematical orbit camera. Independent of Android lifecycle and OpenGL. */
data class OrbitCamera(val azimuth: Float = 0f, val elevation: Float = .24f, val distance: Float = 8.5f) {
    fun adjusted(deltaAzimuth: Float, deltaElevation: Float, deltaZoom: Float): OrbitCamera =
        copy(
            azimuth = azimuth + deltaAzimuth,
            elevation = (elevation + deltaElevation).coerceIn(-1.35f, 1.35f),
            distance = (distance + deltaZoom).coerceIn(4.5f, 18f)
        )
    fun position(): Vec3 =
        Vec3(
            (distance * cos(elevation) * sin(azimuth)),
            (distance * sin(elevation)),
            (distance * cos(elevation) * cos(azimuth))
        )
}
