package universe.xanny.app.render

import android.opengl.GLES20

/** GLES2/OES_depth_texture scene capture; depth is sampled only after unbinding this FBO.
 * The extension and framebuffer completeness are checked at runtime. */
internal class DepthSceneCapture {
    var colorTexture = 0; private set
    var depthTexture = 0; private set
    private var framebuffer = 0
    private var width = 0
    private var height = 0

    fun create(w: Int, h: Int): Boolean {
        dispose()
        if (w !in 1..2048 || h !in 1..2048) return false
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS).orEmpty().split(' ')
        if ("GL_OES_depth_texture" !in extensions) return false
        val ids = IntArray(2)
        GLES20.glGenTextures(2,ids,0)
        colorTexture=ids[0]; depthTexture=ids[1]
        for (i in 0..1) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,ids[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,colorTexture)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,depthTexture)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_DEPTH_COMPONENT,w,h,0,GLES20.GL_DEPTH_COMPONENT,GLES20.GL_UNSIGNED_SHORT,null)
        val fbo = IntArray(1); GLES20.glGenFramebuffers(1,fbo,0);framebuffer=fbo[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,colorTexture,0)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_DEPTH_ATTACHMENT,GLES20.GL_TEXTURE_2D,depthTexture,0)
        val complete=GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)==GLES20.GL_FRAMEBUFFER_COMPLETE
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        if (!complete) {dispose();return false}
        width=w;height=h
        return true
    }
    fun begin() {check(framebuffer!=0); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer);GLES20.glViewport(0,0,width,height)}
    fun end() {GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)}
    fun dispose() {
        if(framebuffer!=0){GLES20.glDeleteFramebuffers(1,intArrayOf(framebuffer),0);framebuffer=0}
        if(colorTexture!=0 || depthTexture!=0){
            val ids=intArrayOf(colorTexture,depthTexture)
            GLES20.glDeleteTextures(2,ids,0);colorTexture=0;depthTexture=0
        }
        width=0;height=0
    }
}
