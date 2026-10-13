package universe.xanny.app.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Genuine OpenGL ES 2 preview scene: indexed sphere meshes, per-fragment diffuse/specular lighting, depth testing,
 * hierarchical orbital motion and drag camera. Not H3, reflected chrome, or character rigs.
 */
class UniverseSurface(context: Context): GLSurfaceView(context) {
    private val previewRenderer = CelestialPreviewRenderer(context)
    private var previousX = 0f
    private var previousY = 0f
    private var previousSpacing = 0f
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(previewRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x; previousY = event.y; previousSpacing = 0f
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) previousSpacing = CameraGestureMath.pointerSpacing(
                    event.getX(0), event.getY(0), event.getX(1), event.getY(1))
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val spacing = CameraGestureMath.pointerSpacing(
                        event.getX(0), event.getY(0), event.getX(1), event.getY(1))
                    if (previousSpacing > 0f) previewRenderer.zoom(previousSpacing, spacing)
                    previousSpacing = spacing
                } else {
                    previewRenderer.drag(event.x - previousX, event.y - previousY)
                    previousX = event.x; previousY = event.y
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                previousSpacing = 0f
                val remainingIndex = if (event.actionIndex == 0) 1 else 0
                if (remainingIndex < event.pointerCount) {
                    previousX = event.getX(remainingIndex)
                    previousY = event.getY(remainingIndex)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> previousSpacing = 0f
        }
        return true
    }
}

private class CelestialPreviewRenderer(context: Context): GLSurfaceView.Renderer {
    private val skinnedAsset: Pair<ImportedTriangleMesh, ImportedSkin>? = try {
        val bytes=context.assets.open("v10_skinned_triangle.glb").use { it.readBytes() }
        GltfMeshDecoder.decode(bytes) to GltfMeshDecoder.decodeSkin(bytes)
    } catch (_: Exception) { null }
    private val animatedPreview=skinnedAsset?.let { AnimatedMeshPreview(it.first,it.second) }
    private val gpuFixture=skinnedAsset?.takeIf { pair ->
        try { GpuSkinningContract.validate(pair.first,pair.second); true }
        catch (_: IllegalArgumentException) { false }
    }
    private val gpuJoints=gpuFixture?.let { pair ->
        ByteBuffer.allocateDirect(pair.first.skinWeights!!.jointIndices.size*4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                pair.first.skinWeights!!.jointIndices.forEach { put(it.toFloat()) };position(0)
            }
    }
    private val gpuWeights=gpuFixture?.let { pair ->
        ByteBuffer.allocateDirect(pair.first.skinWeights!!.weights.size*4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(pair.first.skinWeights!!.weights);position(0)
            }
    }
    // Allocated once with the imported fixture, never per draw frame.
    private val gpuBindPositions=gpuFixture?.let { pair ->
        ByteBuffer.allocateDirect(pair.first.positions.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(pair.first.positions)
                position(0)
            }
    }
    private var gpuProgram=0
    private var gpuPosition=-1
    private var gpuJointsAttr=-1
    private var gpuWeightsAttr=-1
    private var gpuMvp=-1
    private var gpuJoint0=-1
    private var gpuJoint1=-1
    private val animatedPositions=skinnedAsset?.let { ByteBuffer.allocateDirect(it.first.positions.size*4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer() }
    private val animatedIndices=skinnedAsset?.let { ByteBuffer.allocateDirect(it.first.indices.size*2)
        .order(ByteOrder.nativeOrder()).asShortBuffer().apply {put(it.first.indices);position(0)} }
    private val importedMesh: ImportedTriangleMesh? = try {
        context.assets.open("v7_triangle.glb").use { GltfMeshDecoder.decode(it.readBytes()) }
    } catch (_: Exception) { null }
    private val importedPositions = importedMesh?.let { ByteBuffer.allocateDirect(it.positions.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(it.positions);position(0) } }
    private val importedNormals = importedMesh?.normals?.let { ByteBuffer.allocateDirect(it.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(it);position(0) } }
    private val importedIndices = importedMesh?.let { ByteBuffer.allocateDirect(it.indices.size*2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(it.indices);position(0) } }
    @Volatile private var camera = OrbitCamera()
    fun drag(dx: Float, dy: Float) { camera = camera.adjusted(dx * .006f, -dy * .006f, 0f) }
    fun zoom(previousSpacing: Float, currentSpacing: Float) {
        val prior = camera
        camera = prior.adjusted(0f, 0f, CameraGestureMath.zoomDelta(previousSpacing, currentSpacing, prior.distance))
    }
    private val mesh = SphereMesh.create(18, 28)
    private val positions = ByteBuffer.allocateDirect(mesh.vertices.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(mesh.vertices); position(0) }
    private val indices = ByteBuffer.allocateDirect(mesh.indices.size * 2)
        .order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(mesh.indices); position(0) }
    private val projection=FloatArray(16)
    private val view=FloatArray(16)
    private val model=FloatArray(16)
    private val vp=FloatArray(16)
    private val mvp=FloatArray(16)
    private var program=0
    private var normalAttr=-1
    private var useNormalUniform=-1
    private var positionAttr=-1
    private var matrixUniform=-1
    private var colorUniform=-1
    private var modelUniform=-1
    private var eyeUniform=-1
    private var lightUniform=-1
    private var metallicUniform=-1
    private var roughnessUniform=-1
    private var clipUniform=-1
    private var startNanos=0L
    private val vertexShader = "attribute vec3 aNormal; uniform float uUseNormal; attribute vec3 aPosition; uniform mat4 uMvp; uniform mat4 uModel; varying vec3 vNormal; varying vec3 vWorld; void main(){ vec4 w=uModel*vec4(aPosition,1.0); vWorld=w.xyz; vNormal=normalize(mat3(uModel)*mix(aPosition,aNormal,uUseNormal)); gl_Position=uMvp*vec4(aPosition,1.0); }"
    private val fragmentShader = """precision highp float;
        varying vec3 vNormal; varying vec3 vWorld;
        uniform vec3 uColor; uniform vec3 uEye; uniform vec3 uLight;
        uniform float uMetallic; uniform float uRoughness; uniform float uClipEnabled;
        const float PI=3.14159265;
        void main() {
          if (uClipEnabled > 0.5 && vWorld.y < -1.951) discard;
          vec3 N=normalize(vNormal), V=normalize(uEye-vWorld), L=normalize(uLight-vWorld);
          vec3 H=normalize(V+L);
          float NoL=max(dot(N,L),0.0), NoV=max(dot(N,V),0.001), NoH=max(dot(N,H),0.0), VoH=max(dot(V,H),0.0);
          float a=max(uRoughness,0.045); a=a*a; float a2=a*a;
          float d=NoH*NoH*(a2-1.0)+1.0;
          float D=a2/max(PI*d*d,0.000001);
          float k=(uRoughness+1.0);k=k*k/8.0;
          float G=(NoV/(NoV*(1.0-k)+k))*(NoL/(NoL*(1.0-k)+k));
          vec3 F0=mix(vec3(0.04),uColor,uMetallic);
          vec3 F=F0+(vec3(1.0)-F0)*pow(1.0-VoH,5.0);
          vec3 spec=(D*G*F)/max(4.0*NoV*NoL,0.001);
          vec3 kd=(vec3(1.0)-F)*(1.0-uMetallic);
          vec3 radiance=vec3(2.2);
          vec3 direct=(kd*uColor/PI+spec)*radiance*NoL;
          vec3 ambient=uColor*0.035*(1.0-uMetallic);
          vec3 result=(direct+ambient)/(direct+ambient+vec3(1.0));
          gl_FragColor=vec4(pow(max(result,vec3(0.0)),vec3(1.0/2.2)),1.0);
        }"""

    private var volumeProgram=0
    private var volumePosition=-1
    private var volumeEye=-1
    private var volumeRight=-1
    private var volumeUp=-1
    private var volumeForward=-1
    private var volumeAspect=-1
    private var volumeFov=-1
    private var volumeTime=-1
    private var volumeDepth=-1
    private var volumeNear=-1
    private var volumeFar=-1
    private var volumeLightDirection=-1
    private var volumeLightExtinction=-1
    private var volumeShadowSpheres=-1
    private var volumeShadowCount=-1
    private val depthCapture=DepthSceneCapture()
    private var depthAvailable=false
    private var sceneCopyProgram=0
    private var sceneCopyPos=-1
    private var sceneCopyTexture=-1
    private val screenQuad=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply { put(floatArrayOf(-1f,-1f, 1f,-1f, -1f,1f, 1f,1f));position(0) }
    private val reflectionCapture = ReflectionCapture()
    private var planarProgram = 0
    private var planarPosition = -1
    private var planarMvp = -1
    private var planarTexture = -1
    private val planeVerts = java.nio.ByteBuffer.allocateDirect(12 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(-3.3f,-1.95f,-3.3f, 3.3f,-1.95f,-3.3f,
                -3.3f,-1.95f,3.3f, 3.3f,-1.95f,3.3f));position(0)
        }
    private var screenWidth = 1
    private var screenHeight = 1
    private var captureAvailable = false
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        startNanos=System.nanoTime()
        captureAvailable = try { reflectionCapture.create(256); true } catch (_: Exception) { false }
        GLES20.glClearColor(.012f,.015f,.022f,1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        val vs=compile(GLES20.GL_VERTEX_SHADER,vertexShader)
        val fs=compile(GLES20.GL_FRAGMENT_SHADER,fragmentShader)
        program=GLES20.glCreateProgram()
        GLES20.glAttachShader(program,vs)
        GLES20.glAttachShader(program,fs)
        GLES20.glLinkProgram(program)
        val result=IntArray(1)
        GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,result,0)
        if (result[0] != GLES20.GL_TRUE) {
            val details = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteShader(vs); GLES20.glDeleteShader(fs)
            GLES20.glDeleteProgram(program)
            error("Program link failed: $details")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        // Independent preview program; neither PBR chrome nor production GPU skinning.
        val sv=compile(GLES20.GL_VERTEX_SHADER,GpuSkinningContract.vertexShader)
        val sf=compile(GLES20.GL_FRAGMENT_SHADER,GpuSkinningContract.fragmentShader)
        gpuProgram=GLES20.glCreateProgram()
        GLES20.glAttachShader(gpuProgram,sv)
        GLES20.glAttachShader(gpuProgram,sf)
        GLES20.glLinkProgram(gpuProgram)
        val gpuStatus=IntArray(1)
        GLES20.glGetProgramiv(gpuProgram,GLES20.GL_LINK_STATUS,gpuStatus,0)
        if (gpuStatus[0] != GLES20.GL_TRUE) {
            val details = GLES20.glGetProgramInfoLog(gpuProgram)
            GLES20.glDeleteShader(sv); GLES20.glDeleteShader(sf)
            GLES20.glDeleteProgram(gpuProgram)
            error("Program link failed: $details")
        }
        GLES20.glDeleteShader(sv); GLES20.glDeleteShader(sf)
        gpuPosition=GLES20.glGetAttribLocation(gpuProgram,"aPosition")
        gpuJointsAttr=GLES20.glGetAttribLocation(gpuProgram,"aJoints")
        gpuWeightsAttr=GLES20.glGetAttribLocation(gpuProgram,"aWeights")
        gpuMvp=GLES20.glGetUniformLocation(gpuProgram,"uMvp")
        gpuJoint0=GLES20.glGetUniformLocation(gpuProgram,"uJoint0")
        gpuJoint1=GLES20.glGetUniformLocation(gpuProgram,"uJoint1")
        // Bounded 3D density raymarch. No scene-depth occlusion, volumetric shadows or H3 signoff.
        val vv=compile(GLES20.GL_VERTEX_SHADER,VolumetricRaymarch.vertexShader)
        val vf=compile(GLES20.GL_FRAGMENT_SHADER,VolumetricRaymarch.fragmentShader)
        volumeProgram=GLES20.glCreateProgram()
        GLES20.glAttachShader(volumeProgram,vv); GLES20.glAttachShader(volumeProgram,vf)
        GLES20.glLinkProgram(volumeProgram)
        val volumeStatus=IntArray(1)
        GLES20.glGetProgramiv(volumeProgram,GLES20.GL_LINK_STATUS,volumeStatus,0)
        if (volumeStatus[0] != GLES20.GL_TRUE) {
            val details = GLES20.glGetProgramInfoLog(volumeProgram)
            GLES20.glDeleteShader(vv); GLES20.glDeleteShader(vf)
            GLES20.glDeleteProgram(volumeProgram)
            error("Program link failed: $details")
        }
        GLES20.glDeleteShader(vv); GLES20.glDeleteShader(vf)
        volumePosition=GLES20.glGetAttribLocation(volumeProgram,"aScreen")
        volumeEye=GLES20.glGetUniformLocation(volumeProgram,"uEye")
        volumeRight=GLES20.glGetUniformLocation(volumeProgram,"uRight")
        volumeUp=GLES20.glGetUniformLocation(volumeProgram,"uUp")
        volumeForward=GLES20.glGetUniformLocation(volumeProgram,"uForward")
        volumeAspect=GLES20.glGetUniformLocation(volumeProgram,"uAspect")
        volumeFov=GLES20.glGetUniformLocation(volumeProgram,"uTanHalfFov")
        volumeTime=GLES20.glGetUniformLocation(volumeProgram,"uTime")
        volumeDepth=GLES20.glGetUniformLocation(volumeProgram,"uDepth")
        volumeNear=GLES20.glGetUniformLocation(volumeProgram,"uNear")
        volumeFar=GLES20.glGetUniformLocation(volumeProgram,"uFar")
        volumeLightDirection=GLES20.glGetUniformLocation(volumeProgram,"uLightDirection")
        volumeLightExtinction=GLES20.glGetUniformLocation(volumeProgram,"uLightExtinction")
        volumeShadowSpheres=GLES20.glGetUniformLocation(volumeProgram,"uShadowSpheres[0]")
        volumeShadowCount=GLES20.glGetUniformLocation(volumeProgram,"uShadowCount")
        val cv=compile(GLES20.GL_VERTEX_SHADER,VolumetricRaymarch.vertexShader)
        val cf=compile(GLES20.GL_FRAGMENT_SHADER,"precision mediump float; varying vec2 vUv; uniform sampler2D uColor; void main(){gl_FragColor=texture2D(uColor,vUv);}")
        sceneCopyProgram=GLES20.glCreateProgram()
        GLES20.glAttachShader(sceneCopyProgram,cv);GLES20.glAttachShader(sceneCopyProgram,cf)
        GLES20.glLinkProgram(sceneCopyProgram)
        val copyStatus=IntArray(1)
        GLES20.glGetProgramiv(sceneCopyProgram,GLES20.GL_LINK_STATUS,copyStatus,0)
        if (copyStatus[0] != GLES20.GL_TRUE) {
            val details = GLES20.glGetProgramInfoLog(sceneCopyProgram)
            GLES20.glDeleteShader(cv); GLES20.glDeleteShader(cf)
            GLES20.glDeleteProgram(sceneCopyProgram)
            error("Program link failed: $details")
        }
        GLES20.glDeleteShader(cv); GLES20.glDeleteShader(cf)
        sceneCopyPos=GLES20.glGetAttribLocation(sceneCopyProgram,"aScreen")
        sceneCopyTexture=GLES20.glGetUniformLocation(sceneCopyProgram,"uColor")
        // Planar reflection sampling uses mirrored camera capture; it is not full chrome.
        val pv=compile(GLES20.GL_VERTEX_SHADER, "attribute vec3 aPosition; uniform mat4 uMvp; varying vec4 vClip; void main(){vClip=uMvp*vec4(aPosition,1.0); gl_Position=vClip;}")
        val pf=compile(GLES20.GL_FRAGMENT_SHADER, "precision mediump float; varying vec4 vClip; uniform sampler2D uScene; void main(){vec2 uv=(vClip.xy/vClip.w)*0.5+0.5; gl_FragColor=vec4(texture2D(uScene,clamp(uv,0.0,1.0)).rgb*0.74+vec3(0.08),1.0);}")
        planarProgram=GLES20.glCreateProgram()
        GLES20.glAttachShader(planarProgram,pv); GLES20.glAttachShader(planarProgram,pf)
        GLES20.glLinkProgram(planarProgram)
        val planarStatus=IntArray(1)
        GLES20.glGetProgramiv(planarProgram,GLES20.GL_LINK_STATUS,planarStatus,0)
        if (planarStatus[0] != GLES20.GL_TRUE) {
            val details = GLES20.glGetProgramInfoLog(planarProgram)
            GLES20.glDeleteShader(pv); GLES20.glDeleteShader(pf)
            GLES20.glDeleteProgram(planarProgram)
            error("Program link failed: $details")
        }
        GLES20.glDeleteShader(pv);GLES20.glDeleteShader(pf)
        planarPosition=GLES20.glGetAttribLocation(planarProgram,"aPosition")
        planarMvp=GLES20.glGetUniformLocation(planarProgram,"uMvp")
        planarTexture=GLES20.glGetUniformLocation(planarProgram,"uScene")
        positionAttr=GLES20.glGetAttribLocation(program,"aPosition")
        normalAttr=GLES20.glGetAttribLocation(program,"aNormal")
        useNormalUniform=GLES20.glGetUniformLocation(program,"uUseNormal")
        matrixUniform=GLES20.glGetUniformLocation(program,"uMvp")
        colorUniform=GLES20.glGetUniformLocation(program,"uColor")
        modelUniform=GLES20.glGetUniformLocation(program,"uModel")
        eyeUniform=GLES20.glGetUniformLocation(program,"uEye")
        lightUniform=GLES20.glGetUniformLocation(program,"uLight")
        metallicUniform=GLES20.glGetUniformLocation(program,"uMetallic")
        roughnessUniform=GLES20.glGetUniformLocation(program,"uRoughness")
        clipUniform=GLES20.glGetUniformLocation(program,"uClipEnabled")
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0,0,width,height.coerceAtLeast(1))
        Matrix.perspectiveM(projection,0,47f,width.toFloat()/height.coerceAtLeast(1),.1f,100f)
        depthAvailable=depthCapture.create(screenWidth,screenHeight)
    }
    override fun onDrawFrame(gl: GL10?) {
        val frame = PreviewFrameSnapshot.capture((System.nanoTime()-startNanos)/1_000_000_000f)
        // Genuine color+depth capture pass. Independent chrome reflections still require
        // correct per-object capture transforms, sampling, exclusion and GPU qualification.
        if (captureAvailable) {
            reflectionCapture.begin()
            drawScene(mirrored = true, frame = frame)
            reflectionCapture.end(screenWidth,screenHeight)
        }
        if (depthAvailable) depthCapture.begin()
        drawScene(mirrored = false, frame = frame)
        if (captureAvailable) drawPlanarReflection()
        if (depthAvailable) {
            depthCapture.end()
            GLES20.glViewport(0,0,screenWidth,screenHeight)
            drawCapturedScene()
            drawVolumetricPreview(frame)
        }
        // Without OES depth textures render the ordinary scene, without an
        // incorrectly unoccluded atmosphere. No fake H3 fallback.
    }
    private fun drawScene(mirrored: Boolean, frame: PreviewFrameSnapshot) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val p=camera.position()
        // Reflection plane is horizontal at y=-1.95. Mirrored eye and target
        // require inverted up vector to maintain camera orientation.
        val reflectedEyeY = PlanarReflectionMath.shaderEyeHeight(p.y,-1.95f,mirrored)
        if (mirrored) {
            val planeY=-1.95f
            Matrix.setLookAtM(view,0,p.x,reflectedEyeY,p.z,
                0f,2f*planeY,0f,0f,-1f,0f)
        } else {
            Matrix.setLookAtM(view,0,p.x,p.y,p.z,0f,0f,0f,0f,1f,0f)
        }
        Matrix.multiplyMM(vp,0,projection,0,view,0)
        val elapsed=frame.timeSeconds
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(clipUniform,if (mirrored) 1f else 0f)
        // Keep view-dependent Fresnel/specular evaluation aligned with mirrored view.
        GLES20.glUniform3f(eyeUniform,p.x,reflectedEyeY,p.z)
        GLES20.glUniform3f(lightUniform,3.5f,5.0f,6.0f)
        GLES20.glUniform1f(metallicUniform,0.08f)
        GLES20.glUniform1f(roughnessUniform,0.55f)
        GLES20.glEnableVertexAttribArray(positionAttr)
        GLES20.glDisableVertexAttribArray(normalAttr)
        GLES20.glVertexAttrib3f(normalAttr,0f,1f,0f)
        GLES20.glUniform1f(useNormalUniform,0f)
        positions.position(0)
        GLES20.glVertexAttribPointer(positionAttr,3,GLES20.GL_FLOAT,false,0,positions)
        indices.position(0)
        frame.poses.forEach { pose ->
            Matrix.setIdentityM(model,0)
            Matrix.translateM(model,0,pose.location.x,pose.location.y,pose.location.z)
            Matrix.scaleM(model,0,pose.body.radius,pose.body.radius,pose.body.radius)
            Matrix.multiplyMM(mvp,0,vp,0,model,0)
            GLES20.glUniformMatrix4fv(matrixUniform,1,false,mvp,0)
            GLES20.glUniformMatrix4fv(modelUniform,1,false,model,0)
            GLES20.glUniform3f(colorUniform,pose.body.rgb.x,pose.body.rgb.y,pose.body.rgb.z)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,mesh.indices.size,GLES20.GL_UNSIGNED_SHORT,indices)
        }
        // Render validated real GLB triangle via GPU as a clearly identified technical asset sample.
        if (importedMesh != null && importedPositions != null && importedIndices != null) {
            Matrix.setIdentityM(model,0)
            Matrix.translateM(model,0,0f,-1.75f,0f)
            Matrix.multiplyMM(mvp,0,vp,0,model,0)
            GLES20.glUniformMatrix4fv(matrixUniform,1,false,mvp,0)
            GLES20.glUniformMatrix4fv(modelUniform,1,false,model,0)
            val base=importedMesh.material.baseColor
            GLES20.glUniform3f(colorUniform,base[0],base[1],base[2])
            GLES20.glUniform1f(metallicUniform,importedMesh.material.metallic.coerceIn(0f,1f))
            GLES20.glUniform1f(roughnessUniform,importedMesh.material.roughness.coerceIn(0.045f,1f))
            if (importedNormals!=null) {
                GLES20.glEnableVertexAttribArray(normalAttr)
                importedNormals.position(0)
                GLES20.glVertexAttribPointer(normalAttr,3,GLES20.GL_FLOAT,false,0,importedNormals)
                GLES20.glUniform1f(useNormalUniform,1f)
            }
            importedPositions.position(0)
            GLES20.glVertexAttribPointer(positionAttr,3,GLES20.GL_FLOAT,false,0,importedPositions)
            importedIndices.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,importedMesh.indices.size,GLES20.GL_UNSIGNED_SHORT,importedIndices)
            GLES20.glDisableVertexAttribArray(normalAttr)
            GLES20.glUniform1f(useNormalUniform,0f)
        }
        GLES20.glUniform1f(metallicUniform,0.08f)
        GLES20.glUniform1f(roughnessUniform,0.55f)
        // V13 GPU deformation: the GLB's original vertex positions enter a joint-weight vertex shader.
        // The CPU preview is retained for cross-checking and for unsupported GPU cases.
        if (gpuFixture != null && gpuJoints != null && gpuWeights != null && animatedIndices != null) {
            GLES20.glUseProgram(gpuProgram)
            Matrix.setIdentityM(model,0)
            Matrix.translateM(model,0,0f,1.75f,0f)
            Matrix.multiplyMM(mvp,0,vp,0,model,0)
            GLES20.glUniformMatrix4fv(gpuMvp,1,false,mvp,0)
            val joints=GpuSkinningContract.jointMatrices(gpuFixture.second,elapsed)
            GLES20.glUniformMatrix4fv(gpuJoint0,1,false,joints[0],0)
            GLES20.glUniformMatrix4fv(gpuJoint1,1,false,joints[1],0)
            gpuBindPositions!!.position(0)
            GLES20.glEnableVertexAttribArray(gpuPosition)
            GLES20.glEnableVertexAttribArray(gpuJointsAttr)
            GLES20.glEnableVertexAttribArray(gpuWeightsAttr)
            GLES20.glVertexAttribPointer(gpuPosition,3,GLES20.GL_FLOAT,false,0,gpuBindPositions)
            gpuJoints.position(0)
            GLES20.glVertexAttribPointer(gpuJointsAttr,4,GLES20.GL_FLOAT,false,0,gpuJoints)
            gpuWeights.position(0)
            GLES20.glVertexAttribPointer(gpuWeightsAttr,4,GLES20.GL_FLOAT,false,0,gpuWeights)
            animatedIndices.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,gpuFixture.first.indices.size,
                GLES20.GL_UNSIGNED_SHORT,animatedIndices)
            GLES20.glDisableVertexAttribArray(gpuPosition)
            GLES20.glDisableVertexAttribArray(gpuJointsAttr)
            GLES20.glDisableVertexAttribArray(gpuWeightsAttr)
            GLES20.glUseProgram(program)
        }
        // V12 CPU path retained, shifted slightly for side-by-side demonstration.
        if (animatedPreview != null && animatedPositions != null && animatedIndices != null && skinnedAsset != null) {
            val deformed=animatedPreview.positions(elapsed)
            animatedPositions.position(0)
            animatedPositions.put(deformed)
            animatedPositions.position(0)
            Matrix.setIdentityM(model,0)
            Matrix.translateM(model,0,-1.2f,1.75f,0f)
            Matrix.multiplyMM(mvp,0,vp,0,model,0)
            GLES20.glUniformMatrix4fv(matrixUniform,1,false,mvp,0)
            GLES20.glUniformMatrix4fv(modelUniform,1,false,model,0)
            GLES20.glUniform3f(colorUniform,0.85f,0.74f,0.94f)
            GLES20.glUniform1f(useNormalUniform,0f)
            GLES20.glDisableVertexAttribArray(normalAttr)
            GLES20.glVertexAttribPointer(positionAttr,3,GLES20.GL_FLOAT,false,0,animatedPositions)
            animatedIndices.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,skinnedAsset.first.indices.size,
                GLES20.GL_UNSIGNED_SHORT,animatedIndices)
        }
        GLES20.glDisableVertexAttribArray(positionAttr)
    }
    /** A bounded horizontal reflective plane; screen-projected texture from
     * the mirrored camera. No arbitrary chrome materials / correct oblique clipping. */
    private fun drawPlanarReflection() {
        GLES20.glUseProgram(planarProgram)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)
        val p=camera.position()
        Matrix.setLookAtM(view,0,p.x,p.y,p.z,0f,0f,0f,0f,1f,0f)
        Matrix.multiplyMM(vp,0,projection,0,view,0)
        GLES20.glUniformMatrix4fv(planarMvp,1,false,vp,0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,reflectionCapture.textureId)
        GLES20.glUniform1i(planarTexture,0)
        planeVerts.position(0)
        GLES20.glEnableVertexAttribArray(planarPosition)
        GLES20.glVertexAttribPointer(planarPosition,3,GLES20.GL_FLOAT,false,0,planeVerts)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glDisableVertexAttribArray(planarPosition)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }
    /** Copy opaque/reflected scene to default framebuffer before sampling its depth. */
    private fun drawCapturedScene() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(sceneCopyProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,depthCapture.colorTexture)
        GLES20.glUniform1i(sceneCopyTexture,0)
        screenQuad.position(0)
        GLES20.glEnableVertexAttribArray(sceneCopyPos)
        GLES20.glVertexAttribPointer(sceneCopyPos,2,GLES20.GL_FLOAT,false,0,screenQuad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glDisableVertexAttribArray(sceneCopyPos)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
    /** Camera raymarch terminated at depth of opaque scene surfaces. */
    private fun drawVolumetricPreview(frame: PreviewFrameSnapshot) {
        val p=camera.position()
        val fx=-p.x;val fy=-p.y;val fz=-p.z
        val fl=kotlin.math.sqrt(fx*fx+fy*fy+fz*fz).coerceAtLeast(1e-5f)
        val forward=floatArrayOf(fx/fl,fy/fl,fz/fl)
        val rx=-forward[2]; val rz=forward[0]
        val rl=kotlin.math.sqrt(rx*rx+rz*rz).coerceAtLeast(1e-5f)
        val right=floatArrayOf(rx/rl,0f,rz/rl)
        val up=floatArrayOf(-right[2]*forward[1],right[2]*forward[0]-right[0]*forward[2],right[0]*forward[1])
        GLES20.glUseProgram(volumeProgram)
        GLES20.glUniform3f(volumeEye,p.x,p.y,p.z)
        GLES20.glUniform3fv(volumeRight,1,right,0)
        GLES20.glUniform3fv(volumeUp,1,up,0)
        GLES20.glUniform3fv(volumeForward,1,forward,0)
        GLES20.glUniform1f(volumeAspect,screenWidth.toFloat()/screenHeight.toFloat())
        GLES20.glUniform1f(volumeFov,kotlin.math.tan(Math.toRadians(47.0/2.0)).toFloat())
        GLES20.glUniform1f(volumeTime,frame.timeSeconds)
        GLES20.glUniform1f(volumeNear,0.1f)
        GLES20.glUniform1f(volumeFar,100f)
        GLES20.glUniform3f(volumeLightDirection,0.35f,0.85f,0.40f)
        GLES20.glUniform1f(volumeLightExtinction,0.95f)
        // V22: same animated scene body state as drawScene; bounded analytic
        // approximation uses each visible sphere's center and radius.
        val blockers=frame.shadowSpheres
        val packed=FloatArray(SceneShadowSync.MAX_OCCLUDERS*4)
        blockers.forEachIndexed { index, sphere ->
            val offset=index*4
            packed[offset]=sphere.x; packed[offset+1]=sphere.y
            packed[offset+2]=sphere.z; packed[offset+3]=sphere.radius
        }
        GLES20.glUniform4fv(volumeShadowSpheres,SceneShadowSync.MAX_OCCLUDERS,packed,0)
        GLES20.glUniform1f(volumeShadowCount,blockers.size.toFloat())
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,depthCapture.depthTexture)
        GLES20.glUniform1i(volumeDepth,1)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA)
        screenQuad.position(0)
        GLES20.glEnableVertexAttribArray(volumePosition)
        GLES20.glVertexAttribPointer(volumePosition,2,GLES20.GL_FLOAT,false,0,screenQuad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glDisableVertexAttribArray(volumePosition)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
    private fun compile(type: Int, source: String): Int {
        val s=GLES20.glCreateShader(type)
        GLES20.glShaderSource(s,source)
        GLES20.glCompileShader(s)
        val status=IntArray(1)
        GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,status,0)
        if (status[0] != GLES20.GL_TRUE) {
            val details = GLES20.glGetShaderInfoLog(s)
            GLES20.glDeleteShader(s)
            val stage = if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment"
            error("$stage shader compilation failed: $details")
        }
        return s
    }
}

internal data class SphereMesh(val vertices: FloatArray, val indices: ShortArray) {
    companion object {
        fun create(stacks: Int, slices: Int): SphereMesh {
            require(stacks >= 3 && slices >= 3)
            require((stacks+1)*(slices+1) <= 65535)
            val verts=ArrayList<Float>()
            val tris=ArrayList<Short>()
            for (stack in 0..stacks) {
                val theta=PI*stack/stacks
                val y=cos(theta).toFloat()
                val ring=sin(theta).toFloat()
                for (slice in 0..slices) {
                    val phi=2*PI*slice/slices
                    verts.add((ring*cos(phi)).toFloat())
                    verts.add(y)
                    verts.add((ring*sin(phi)).toFloat())
                }
            }
            for (stack in 0 until stacks) for (slice in 0 until slices) {
                val a=stack*(slices+1)+slice
                val b=a+slices+1
                // Outward winding for OpenGL CCW-front faces.
                tris.add(a.toShort()); tris.add((a+1).toShort()); tris.add(b.toShort())
                tris.add((a+1).toShort()); tris.add((b+1).toShort()); tris.add(b.toShort())
            }
            return SphereMesh(verts.toFloatArray(),tris.toShortArray())
        }
    }
}
