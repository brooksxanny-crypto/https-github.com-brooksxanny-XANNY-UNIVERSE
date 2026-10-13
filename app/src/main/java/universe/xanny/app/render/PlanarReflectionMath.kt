package universe.xanny.app.render

/** Geometry used by the preview planar-reflection pass. */
object PlanarReflectionMath {
    fun mirroredHeight(eyeY: Float, planeY: Float): Float {
        require(eyeY.isFinite() && planeY.isFinite())
        return 2f * planeY - eyeY
    }
    /** Eye Y for both view matrix and PBR view-dependent lighting. */
    fun shaderEyeHeight(eyeY: Float, planeY: Float, mirrored: Boolean): Float =
        if (mirrored) mirroredHeight(eyeY, planeY) else eyeY.also { require(it.isFinite()) }

    fun projectiveUv(clipX: Float, clipY: Float, clipW: Float): Pair<Float, Float> {
        require(clipX.isFinite() && clipY.isFinite() && clipW.isFinite() && clipW > 0f)
        return Pair((clipX / clipW * .5f + .5f).coerceIn(0f,1f),
                    (clipY / clipW * .5f + .5f).coerceIn(0f,1f))
    }
}
