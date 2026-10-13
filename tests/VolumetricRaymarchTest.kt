import universe.xanny.app.render.*
fun main(){
 VolumetricRaymarch.validate()
 check(VolumetricRaymarch.steps == 48)
 check(listOf("for(int i=0;i<STEPS;i++)", "uEye+dir*t", "transmittance", "density(vec3 p)").all{VolumetricRaymarch.fragmentShader.contains(it)})
 var rejected=false
 try { VolumetricRaymarch.validate(0) } catch (_:IllegalArgumentException){rejected=true}
 check(rejected)
 println("PASS V18 bounded raymarch source contract")
}