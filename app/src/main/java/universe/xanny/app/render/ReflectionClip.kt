package universe.xanny.app.render

/** World-space reflection clipping math for a horizontal plane. */
object ReflectionClip {
    fun survives(pointY:Float,planeY:Float,epsilon:Float=0.001f):Boolean {
        require(pointY.isFinite() && planeY.isFinite() && epsilon.isFinite() && epsilon>=0f)
        return pointY>=planeY-epsilon
    }
}
