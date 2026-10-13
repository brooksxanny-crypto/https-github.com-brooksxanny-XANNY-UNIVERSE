import universe.xanny.app.render.*
fun main() {
  val field=EdgeVolumeField(VolumeBounds(3.7f,2.5f,4f))
  val dir=Vec3(0f,1f,0f)
  val clear=VolumeLight.visibility(field,Vec3(0f,0f,0f),dir,0f)
  val dense=VolumeLight.visibility(field,Vec3(-3.68f,0f,0f),dir,0f)
  check(clear>dense && dense in 0f..1f)
  check(VolumeLight.visibility(field,Vec3(-3.68f,0f,0f),dir,0f,0f)==1f)
  var rejected=false
  try { VolumeLight.visibility(field,Vec3(0f,0f,0f),dir,0f,-1f) } catch(_:IllegalArgumentException) { rejected=true }
  check(rejected)
  val shader=VolumetricRaymarch.fragmentShader
  check("lightVisibility" in shader && "uLightDirection" in shader && "uLightExtinction" in shader)
  println("PASS V20: bounded volumetric self-shadow reference and shader contract")
}
