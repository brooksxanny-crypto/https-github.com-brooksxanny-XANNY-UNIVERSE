import universe.xanny.app.render.PlanarReflectionMath
import kotlin.math.abs
fun main() {
  check(abs(PlanarReflectionMath.mirroredHeight(3f,-2f)+7f) < 1e-5)
  check(abs(PlanarReflectionMath.mirroredHeight(-7f,-2f)-3f) < 1e-5)
  check(PlanarReflectionMath.projectiveUv(0f,0f,1f) == Pair(.5f,.5f))
  check(PlanarReflectionMath.projectiveUv(100f,-100f,1f) == Pair(1f,0f))
  var denied=false
  try { PlanarReflectionMath.projectiveUv(0f,0f,0f) } catch (_:IllegalArgumentException) {denied=true}
  check(denied)
  println("PASS planar reflection math")
}
