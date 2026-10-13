import universe.xanny.app.render.*
fun main() {
    for(t in listOf(0f, .25f, 4f, 150f)) {
        val f=PreviewFrameSnapshot.capture(t)
        check(f.timeSeconds==t)
        check(f.poses.size==3 && f.shadowSpheres.size==3)
        check(f.shadowSpheres==SceneShadowSync.fromPoses(f.poses))
        check(f.poses==PreviewScene.poses(t))
    }
    for (invalid in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f)) {
        try { PreviewFrameSnapshot.capture(invalid); error("failed reject") } catch(_:IllegalArgumentException) { }
    }
    println("PASS V23 single animation-frame snapshot for scene and shadows")
}
