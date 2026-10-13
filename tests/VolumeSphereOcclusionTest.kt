import universe.xanny.app.render.*
fun main() {
    val s=ShadowSphere(0f,0f,0f,1f)
    check(VolumeSphereOcclusion.blocksLight(0f,0f,-3f,0f,0f,1f,4f,s))
    check(!VolumeSphereOcclusion.blocksLight(0f,0f,-3f,0f,1f,0f,4f,s))
    check(!VolumeSphereOcclusion.blocksLight(0f,0f,-3f,0f,0f,1f,1f,s))
    check(VolumeSphereOcclusion.blocksLight(0f,0f,0f,1f,0f,0f,4f,s))
    try { ShadowSphere(0f,0f,0f,0f); error("expected reject") } catch(_:IllegalArgumentException) {}
    check("oneSphereBlocksLight" in VolumetricRaymarch.fragmentShader)
    check("sceneBlocksLight" in VolumetricRaymarch.fragmentShader)
    check("uShadowSpheres[3]" in VolumetricRaymarch.fragmentShader)
    check("uShadowCount" in VolumetricRaymarch.fragmentShader)
    println("PASS V21 analytic sphere occlusion and shader contract")
}
