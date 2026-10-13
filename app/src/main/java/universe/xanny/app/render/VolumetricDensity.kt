package universe.xanny.app.render

import kotlin.math.*

/** Spatially volumetric density field prototype; no H3 GPU raymarching yet. */
data class VolumeBounds(val halfWidth: Float, val halfHeight: Float, val halfDepth: Float) {
    init { require(halfWidth>0f && halfHeight>0f && halfDepth>0f && listOf(halfWidth,halfHeight,halfDepth).all { it.isFinite() }) }
}

/** Eight edge/corner-adjacent regions represented in real 3D coordinates. */
class EdgeVolumeField(private val bounds: VolumeBounds, private val thickness: Float = .7f) {
    init { require(thickness.isFinite() && thickness > 0f) }
    private fun smooth(t: Float) : Float { val u=t.coerceIn(0f,1f); return u*u*(3f-2f*u) }
    private fun nearEdge(distanceToEdge:Float)=smooth((thickness-distanceToEdge)/thickness)
    fun density(p: Vec3, timeSeconds: Float): Float {
        require(listOf(p.x,p.y,p.z,timeSeconds).all { it.isFinite() })
        if (abs(p.x)>bounds.halfWidth || abs(p.y)>bounds.halfHeight || abs(p.z)>bounds.halfDepth) return 0f
        val left=nearEdge(p.x+bounds.halfWidth)
        val right=nearEdge(bounds.halfWidth-p.x)
        val bottom=nearEdge(p.y+bounds.halfHeight)
        val top=nearEdge(bounds.halfHeight-p.y)
        // 4 sides and 4 corners, not a flattened overlay.
        val sides=max(max(left,right),max(top,bottom))
        val corners=max(max(left*top,right*top),max(left*bottom,right*bottom))
        val depthEnvelope=0.35f+0.65f*(1f-(abs(p.z)/bounds.halfDepth))
        val motion=.82f+.18f*sin(p.x*2.1f+p.z*1.7f+timeSeconds*.6f)*cos(p.y*1.4f-timeSeconds*.4f)
        return ((.65f*sides+.35f*corners)*depthEnvelope*motion).coerceIn(0f,1f)
    }
    /** Beer-Lambert extinction integrated along a 3D ray, step bounded for prototype tests. */
    fun transmittance(origin:Vec3, direction:Vec3, length:Float, steps:Int, timeSeconds:Float): Float {
        require(length.isFinite() && length>=0f && steps in 1..2048)
        require(listOf(direction.x,direction.y,direction.z).all{it.isFinite()})
        val magnitude=sqrt(direction.x*direction.x+direction.y*direction.y+direction.z*direction.z)
        require(magnitude>1e-6f)
        val ds=length/steps
        var opticalDepth=0f
        for(i in 0 until steps) {
            val t=(i+.5f)*ds/magnitude
            val p=Vec3(origin.x+direction.x*t, origin.y+direction.y*t, origin.z+direction.z*t)
            opticalDepth+=density(p,timeSeconds)*ds
        }
        return exp(-opticalDepth).coerceIn(0f,1f)
    }
}
