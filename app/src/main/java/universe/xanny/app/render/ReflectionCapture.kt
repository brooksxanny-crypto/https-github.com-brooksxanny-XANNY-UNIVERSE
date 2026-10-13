package universe.xanny.app.render

import android.opengl.GLES20

/** Genuine offscreen scene-color + depth render target. This is capture infrastructure,
 * NOT yet a reflection renderer: the captured texture is not projected or sampled by chrome. */
internal class ReflectionCapture {
    var textureId: Int = 0; private set
    private var framebuffer = 0
    private var depthbuffer = 0
    private var dimension = 0
    fun create(size: Int) {
        require(size in 64..1024 && size and (size - 1) == 0)
        dispose()
        dimension = size
        val id = IntArray(1)
        GLES20.glGenTextures(1,id,0); textureId=id[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,size,size,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null)
        GLES20.glGenRenderbuffers(1,id,0);depthbuffer=id[0]
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,depthbuffer)
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,GLES20.GL_DEPTH_COMPONENT16,size,size)
        GLES20.glGenFramebuffers(1,id,0);framebuffer=id[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,textureId,0)
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,GLES20.GL_DEPTH_ATTACHMENT,GLES20.GL_RENDERBUFFER,depthbuffer)
        val status=GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,0)
        if (status!=GLES20.GL_FRAMEBUFFER_COMPLETE) {
            dispose();error("Reflection capture FBO unsupported: $status")
        }
    }
    fun begin() { check(framebuffer!=0); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer); GLES20.glViewport(0,0,dimension,dimension) }
    fun end(width:Int,height:Int) { GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0); GLES20.glViewport(0,0,width,height) }
    fun dispose() {
        val id=IntArray(1)
        if(framebuffer!=0){id[0]=framebuffer; GLES20.glDeleteFramebuffers(1,id,0);framebuffer=0}
        if(depthbuffer!=0){id[0]=depthbuffer; GLES20.glDeleteRenderbuffers(1,id,0);depthbuffer=0}
        if(textureId!=0){id[0]=textureId; GLES20.glDeleteTextures(1,id,0);textureId=0}
    }
}
