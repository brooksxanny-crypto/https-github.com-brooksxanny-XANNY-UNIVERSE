import universe.xanny.app.render.*
import kotlin.math.abs
fun main() {
 val f=EdgeVolumeField(VolumeBounds(4f,3f,2f))
 val zero=Vec3(0f,0f,0f);val left=Vec3(-3.99f,0f,0f);val corner=Vec3(-3.99f,2.99f,0f)
 check(f.density(zero,0f)==0f)
 check(f.density(left,0f)>0f)
 check(f.density(corner,0f)>0f)
 check(f.density(Vec3(5f,0f,0f),0f)==0f)
 check(abs(f.density(left,0f)-f.density(left,3f))>1e-5f)
 check(f.density(Vec3(-3.99f,0f,1.8f),0f)!=f.density(left,0f))
 val clear=f.transmittance(zero,Vec3(0f,0f,1f),1f,24,0f)
 val fog=f.transmittance(left,Vec3(0f,0f,1f),1f,24,0f)
 check(clear>fog && fog in 0f..1f)
 check(ReflectionClip.survives(0f,-1.95f))
 check(!ReflectionClip.survives(-2.2f,-1.95f))
 var failed=false
 try{ f.transmittance(zero,zero,1f,8,0f) }catch(_:IllegalArgumentException){failed=true}
 check(failed)
 println("PASS: volumetric spatial regions, temporal variation, depth dependence, ray extinction and clip policy")
}
