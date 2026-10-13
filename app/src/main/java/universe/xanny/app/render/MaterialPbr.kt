package universe.xanny.app.render

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow

/** Simple direct-light Cook-Torrance GGX reference; no scene reflections or full glTF PBR. */
data class PbrFactors(val metallic: Float, val roughness: Float) {
    init { require(metallic.isFinite() && metallic in 0f..1f); require(roughness.isFinite() && roughness in 0f..1f) }
    val safeRoughness: Float get() = roughness.coerceAtLeast(0.045f)
}
object DirectLightBrdf {
    fun distributionGgx(noH: Float, roughness: Float): Float {
        val a=roughness.coerceAtLeast(.045f).pow(2)
        val a2=a*a
        val d=noH.coerceIn(0f,1f).let { it*it*(a2-1f)+1f }
        return (a2/(PI.toFloat()*d*d).coerceAtLeast(1e-8f))
    }
    fun geometrySmith(noV: Float, noL: Float, roughness: Float): Float {
        val k=(roughness+1f).let { it*it/8f }
        fun g(n: Float)=n.coerceIn(0f,1f)/(n.coerceIn(0f,1f)*(1f-k)+k)
        return g(noV)*g(noL)
    }
    fun fresnel(cosine: Float, f0: Float): Float = f0+(1f-f0)*(1f-cosine.coerceIn(0f,1f)).pow(5)
}
