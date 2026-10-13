import universe.xanny.app.render.*
import kotlin.math.abs
fun main() {
    check(abs(CameraGestureMath.pointerSpacing(0f,0f,3f,4f)-5f)<1e-5f)
    check(CameraGestureMath.zoomDelta(100f,200f,8f)<0f)
    check(CameraGestureMath.zoomDelta(200f,100f,8f)>0f)
    check(CameraGestureMath.zoomDelta(0f,100f,8f)==0f)
    val camera=OrbitCamera().adjusted(0f,0f,CameraGestureMath.zoomDelta(100f,200f,8f))
    check(camera.distance >= 4.5f && camera.distance < 8.5f)
    var rejected=false
    try { CameraGestureMath.pointerSpacing(Float.NaN,0f,0f,0f) } catch (_: IllegalArgumentException) { rejected=true }
    check(rejected)
    println("PASS CameraGestureMathTest")
}
