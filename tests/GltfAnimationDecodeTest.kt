import universe.xanny.app.render.*
import java.io.File
import kotlin.math.abs
fun main(args:Array<String>){
 val bytes=File(args[0]).readBytes()
 val anim=GltfAnimationDecoder.decode(bytes)
 check(anim.name=="test_move")
 check(abs(anim.evaluate(1f,listOf(JointTRS()))[0].translation.x-1f)<.0001f)
 var failed=false
 try { GltfAnimationDecoder.decode(bytes.copyOfRange(0,bytes.size-4)) } catch(_:Exception){failed=true}
 check(failed)
 println("PASS GLB animation channel decode, interpolation and truncated-file rejection")
}
