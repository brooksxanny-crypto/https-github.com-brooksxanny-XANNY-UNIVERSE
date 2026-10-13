import universe.xanny.app.render.*
fun main() {
 val identity=(0 until 16).map { if(it%5==0) 1f else 0f }
 val skeleton=SkeletonDefinition(listOf(JointDefinition("root",null,identity), JointDefinition("arm",0,identity)))
 check(skeleton.rootJointIndices==listOf(0))
 check(runCatching { SkeletonDefinition(listOf(JointDefinition("broken",1,identity))) }.isFailure)
 check(runCatching { SkeletonDefinition(listOf(JointDefinition("bad",null,listOf(1f)))) }.isFailure)
 println("PASS: skeleton hierarchy and inverse-bind validation")
}
