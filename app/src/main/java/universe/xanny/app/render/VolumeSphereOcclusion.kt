package universe.xanny.app.render

import kotlin.math.sqrt

/** Bounded analytic scene-object light blockers, not a full shadow map. */
data class ShadowSphere(val x: Float, val y: Float, val z: Float, val radius: Float) {
    init { require(listOf(x,y,z,radius).all { it.isFinite() } && radius > 0f) }
}
object VolumeSphereOcclusion {
    fun blocksLight(px: Float, py: Float, pz: Float, dx: Float, dy: Float, dz: Float,
                    maxDistance: Float, sphere: ShadowSphere): Boolean {
        require(listOf(px,py,pz,dx,dy,dz,maxDistance).all { it.isFinite() } && maxDistance > 0f)
        val len2 = dx*dx+dy*dy+dz*dz
        require(len2 > 1.0e-10f)
        val inv=1f/sqrt(len2)
        val ux=dx*inv; val uy=dy*inv; val uz=dz*inv
        val ox=px-sphere.x; val oy=py-sphere.y; val oz=pz-sphere.z
        val b=ox*ux+oy*uy+oz*uz
        val c=ox*ox+oy*oy+oz*oz-sphere.radius*sphere.radius
        val disc=b*b-c
        if(disc < 0f) return false
        val q=sqrt(disc)
        return -b+q > 0.001f && -b-q < maxDistance
    }
}
