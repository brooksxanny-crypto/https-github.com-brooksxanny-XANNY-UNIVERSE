import universe.xanny.app.render.PlanarReflectionMath
import kotlin.math.abs
fun main() {
    val plane=-1.95f
    for (eye in listOf(-7f,-1.95f,0f,2f,9f)) {
        val reflected=PlanarReflectionMath.shaderEyeHeight(eye,plane,true)
        check(abs((eye+reflected)-2f*plane)<1e-5f)
        check(PlanarReflectionMath.shaderEyeHeight(eye,plane,false)==eye)
        check(abs(PlanarReflectionMath.shaderEyeHeight(reflected,plane,true)-eye)<1e-5f)
    }
    var rejected=false
    try {PlanarReflectionMath.shaderEyeHeight(Float.NaN,plane,true)}catch(_:IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS V33 mirrored view eye and shader eye geometry")
}
