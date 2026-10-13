import universe.xanny.app.render.DepthRayLimit
import kotlin.math.abs
fun main() {
    fun eq(a:Float,b:Float)=check(abs(a-b)<0.001f){"$a != $b"}
    eq(DepthRayLimit.distance(0f,.1f,100f,1f),.1f)
    eq(DepthRayLimit.distance(1f,.1f,100f,1f),13f)
    eq(DepthRayLimit.distance(.5f,.1f,100f,1f),.1998f)
    eq(DepthRayLimit.distance(.5f,.1f,100f,.5f),.3996f)
    var rejected=false
    try{DepthRayLimit.distance(Float.NaN,.1f,100f,1f)}catch(_:IllegalArgumentException){rejected=true}
    check(rejected)
    println("PASS V19 depth reconstruction and ray limit")
}
