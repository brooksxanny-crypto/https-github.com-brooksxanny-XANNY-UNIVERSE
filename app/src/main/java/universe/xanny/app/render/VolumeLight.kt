package universe.xanny.app.render

import kotlin.math.exp

/** JVM reference for bounded self-shadow light transmittance in the volume. */
object VolumeLight {
    fun visibility(field: EdgeVolumeField, point: Vec3, towardLight: Vec3,
                   timeSeconds: Float, extinction: Float = 0.95f): Float {
        require(extinction.isFinite() && extinction >= 0f)
        val distance = 8 * 0.32f
        val t=field.transmittance(point,towardLight,distance,8,timeSeconds)
        return if(extinction==0f) 1f else exp(kotlin.math.ln(t.coerceAtLeast(1e-30f))*extinction).coerceIn(0f,1f)
    }
}
