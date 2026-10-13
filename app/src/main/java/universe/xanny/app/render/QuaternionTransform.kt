package universe.xanny.app.render

import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos

data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    init { require(listOf(x,y,z,w).all { it.isFinite() }) }
    fun normalized(): Quaternion {
        val m = sqrt(x*x+y*y+z*z+w*w)
        require(m > 0.000001f) { "Zero length quaternion" }
        return Quaternion(x/m,y/m,z/m,w/m)
    }
    operator fun times(b: Quaternion): Quaternion = Quaternion(
        w*b.x+x*b.w+y*b.z-z*b.y,
        w*b.y-x*b.z+y*b.w+z*b.x,
        w*b.z+x*b.y-y*b.x+z*b.w,
        w*b.w-x*b.x-y*b.y-z*b.z)
    fun rotate(p: Vec3): Vec3 {
        val q=normalized()
        val v=Quaternion(p.x,p.y,p.z,0f)
        val t=q*v*Quaternion(-q.x,-q.y,-q.z,q.w)
        return Vec3(t.x,t.y,t.z)
    }
    companion object {
        val IDENTITY=Quaternion(0f,0f,0f,1f)
        fun axisAngle(axis: Vec3, radians: Float): Quaternion {
            require(radians.isFinite())
            val n=sqrt(axis.x*axis.x+axis.y*axis.y+axis.z*axis.z)
            require(n > 0.000001f)
            val s=sin(radians/2)/n
            return Quaternion(axis.x*s,axis.y*s,axis.z*s,cos(radians/2)).normalized()
        }
    }
}

data class Transform3D(val translation: Vec3, val rotation: Quaternion, val scale: Float) {
    init { require(scale.isFinite() && scale > 0f); require(listOf(translation.x,translation.y,translation.z).all { it.isFinite() }) }
    fun child(local: Transform3D): Transform3D {
        val offset=rotation.rotate(Vec3(local.translation.x*scale,local.translation.y*scale,local.translation.z*scale))
        return Transform3D(Vec3(translation.x+offset.x,translation.y+offset.y,translation.z+offset.z),
            (rotation*local.rotation).normalized(),scale*local.scale)
    }
}

class TransformHierarchy(private val nodes: Map<String, Pair<String?,Transform3D>>) {
    init {
        require(nodes.keys.all { it.isNotBlank() })
        nodes.forEach { (id,value) -> require(value.first==null || value.first in nodes); resolve(id) }
    }
    fun resolve(id: String): Transform3D {
        val path=mutableSetOf<String>()
        fun world(current:String):Transform3D {
            require(path.add(current)) { "Cycle at $current" }
            val entry=nodes[current] ?: throw IllegalArgumentException("Unknown node: $current")
            val result=entry.first?.let { world(it).child(entry.second) } ?: entry.second
            path.remove(current)
            return result
        }
        return world(id)
    }
}
