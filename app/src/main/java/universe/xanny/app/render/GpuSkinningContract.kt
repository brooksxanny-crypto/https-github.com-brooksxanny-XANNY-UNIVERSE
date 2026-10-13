package universe.xanny.app.render

/** GLES2-compatible demonstration: exactly two joints, four weighted influences per vertex. */
object GpuSkinningContract {
    const val MAX_JOINTS = 2
    const val vertexShader = """
        attribute vec3 aPosition;
        attribute vec4 aJoints;
        attribute vec4 aWeights;
        uniform mat4 uMvp;
        uniform mat4 uJoint0;
        uniform mat4 uJoint1;
        varying float vShade;
        void main() {
            vec4 p=vec4(aPosition,1.0);
            vec4 skinned=vec4(0.0);
            for(int k=0;k<4;k++) {
                float index=aJoints[k];
                float weight=aWeights[k];
                skinned += weight * (index < 0.5 ? (uJoint0*p) : (uJoint1*p));
            }
            gl_Position=uMvp*skinned;
            vShade=0.7+0.3*clamp(skinned.y,0.0,1.0);
        }
    """
    const val fragmentShader = """
        precision mediump float;
        varying float vShade;
        void main() { gl_FragColor=vec4(vec3(0.81,0.69,0.91)*vShade,1.0); }
    """
    fun validate(mesh: ImportedTriangleMesh, skin: ImportedSkin) {
        require(skin.nodeIndices.size == MAX_JOINTS) { "V13 preview supports exactly two joints" }
        val influences=requireNotNull(mesh.skinWeights)
        require(influences.vertexCount==mesh.positions.size/3)
        require(influences.jointIndices.all { it in 0 until MAX_JOINTS })
    }
    fun jointMatrices(skin: ImportedSkin, seconds: Float): Array<FloatArray> {
        require(seconds.isFinite())
        val displacement=kotlin.math.sin(seconds.toDouble()).toFloat()*0.5f
        return Array(MAX_JOINTS) { i ->
            val world=if(i==1) Mat4.translation(displacement,0f,0f) else Mat4.IDENTITY
            (world*skin.inverseBindMatrices[i]).m
        }
    }
}
