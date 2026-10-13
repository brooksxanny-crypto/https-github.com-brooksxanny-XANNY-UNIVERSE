package universe.xanny.app.render

import kotlin.math.abs
import kotlin.math.sqrt

/** Column-major 4x4 affine matrices, compatible with glTF coordinate conventions. */
class Mat4(val m: FloatArray) {
    init { require(m.size == 16 && m.all { it.isFinite() }) }
    operator fun times(other: Mat4): Mat4 {
        val out = FloatArray(16)
        for (col in 0..3) for (row in 0..3) for (k in 0..3)
            out[col*4+row] += m[k*4+row]*other.m[col*4+k]
        return Mat4(out)
    }
    fun transformPoint(v: Vec3): Vec3 {
        val x=m[0]*v.x+m[4]*v.y+m[8]*v.z+m[12]
        val y=m[1]*v.x+m[5]*v.y+m[9]*v.z+m[13]
        val z=m[2]*v.x+m[6]*v.y+m[10]*v.z+m[14]
        val w=m[3]*v.x+m[7]*v.y+m[11]*v.z+m[15]
        require(abs(w)>1e-7f)
        return Vec3(x/w,y/w,z/w)
    }
    fun transformDirection(v: Vec3): Vec3 = Vec3(
        m[0]*v.x+m[4]*v.y+m[8]*v.z,
        m[1]*v.x+m[5]*v.y+m[9]*v.z,
        m[2]*v.x+m[6]*v.y+m[10]*v.z
    )
    companion object {
        val IDENTITY get() = Mat4(floatArrayOf(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, 0f,0f,0f,1f))
        fun translation(x:Float,y:Float,z:Float) = Mat4(floatArrayOf(
            1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f, x,y,z,1f
        ))
    }
}

/** One mesh's JOINTS_0 and WEIGHTS_0 attributes: four influences per vertex.
 * Only normalized linear-blend CPU deformation, not dual quaternion/GPU or full glTF animation. */
data class SkinWeights(val jointIndices: IntArray, val weights: FloatArray, val vertexCount: Int) {
    init {
        require(vertexCount > 0 && jointIndices.size==vertexCount*4 && weights.size==vertexCount*4)
        require(jointIndices.all { it>=0 })
        require(weights.all { it.isFinite() && it>=0f })
        for (i in 0 until vertexCount) {
            val total=(0..3).sumOf { weights[i*4+it].toDouble() }
            require(abs(total-1.0) <= .002) { "Each vertex must have normalized skin weights" }
        }
    }
}

object CpuSkinning {
    /** jointWorldMatrices and inverseBindMatrices must be in the same skin-joint order. */
    fun deform(positions: FloatArray, influences: SkinWeights,
               jointWorldMatrices: List<Mat4>, inverseBindMatrices: List<Mat4>): FloatArray {
        require(positions.size == influences.vertexCount*3)
        require(jointWorldMatrices.isNotEmpty() && jointWorldMatrices.size == inverseBindMatrices.size)
        val transforms=jointWorldMatrices.indices.map { jointWorldMatrices[it]*inverseBindMatrices[it] }
        val result=FloatArray(positions.size)
        for (i in 0 until influences.vertexCount) {
            val v=Vec3(positions[i*3],positions[i*3+1],positions[i*3+2])
            require(listOf(v.x,v.y,v.z).all { it.isFinite() })
            var x=0f; var y=0f; var z=0f
            for (slot in 0..3) {
                val weight=influences.weights[i*4+slot]
                if (weight==0f) continue
                val index=influences.jointIndices[i*4+slot]
                require(index in transforms.indices) { "Skin references missing joint" }
                val transformed=transforms[index].transformPoint(v)
                x+=transformed.x*weight; y+=transformed.y*weight; z+=transformed.z*weight
            }
            require(x.isFinite() && y.isFinite() && z.isFinite())
            result[i*3]=x;result[i*3+1]=y;result[i*3+2]=z
        }
        return result
    }
}
