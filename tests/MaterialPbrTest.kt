import universe.xanny.app.render.*
import kotlin.math.abs
fun main(){
 val dielectric=PbrFactors(0f,0f);check(abs(dielectric.safeRoughness-.045f)<.0001f)
 val metallic=PbrFactors(1f,.35f);check(metallic.safeRoughness==.35f)
 check(DirectLightBrdf.distributionGgx(1f,.2f)>DirectLightBrdf.distributionGgx(1f,.8f))
 check(DirectLightBrdf.fresnel(0f,.04f)>DirectLightBrdf.fresnel(1f,.04f))
 check(DirectLightBrdf.geometrySmith(.6f,.7f,.5f) in 0f..1f)
 var rejected=false;try{PbrFactors(1.1f,.5f)}catch(_:IllegalArgumentException){rejected=true};check(rejected)
 println("PASS V14 direct-light microfacet factors and input validation")
}
