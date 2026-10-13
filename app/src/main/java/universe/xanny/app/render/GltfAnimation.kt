package universe.xanny.app.render

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** glTF2 subset: linear/step translation, rotation and scale; no CUBICSPLINE or morph weights. */
data class JointTRS(val translation: Vec3 = Vec3(0f,0f,0f), val rotation: Quaternion = Quaternion.IDENTITY, val scale: Vec3 = Vec3(1f,1f,1f)) {
    fun matrix(): Mat4 {
        val q=rotation.normalized(); val x=q.x;val y=q.y;val z=q.z;val w=q.w
        return Mat4(floatArrayOf(
            (1f-2f*(y*y+z*z))*scale.x, (2f*(x*y+z*w))*scale.x, (2f*(x*z-y*w))*scale.x, 0f,
            (2f*(x*y-z*w))*scale.y, (1f-2f*(x*x+z*z))*scale.y, (2f*(y*z+x*w))*scale.y, 0f,
            (2f*(x*z+y*w))*scale.z, (2f*(y*z-x*w))*scale.z, (1f-2f*(x*x+y*y))*scale.z, 0f,
            translation.x,translation.y,translation.z,1f
        ))
    }
}
data class AnimationChannel(val nodeIndex: Int, val path: String, val interpolation: String, val times: FloatArray, val values: FloatArray) {
    init {
        require(nodeIndex>=0 && path in setOf("translation","rotation","scale"))
        require(interpolation in setOf("LINEAR","STEP"))
        require(times.isNotEmpty() && times.all { it.isFinite() && it>=0f })
        require((1 until times.size).all {times[it]>times[it-1]})
        require(values.size==times.size*(if(path=="rotation")4 else 3) && values.all {it.isFinite()})
    }
    fun sample(time:Float): FloatArray {
        require(time.isFinite())
        val width=if(path=="rotation")4 else 3
        val last=times.lastIndex
        if(time<=times[0]) return values.copyOfRange(0,width)
        if(time>=times[last]) return values.copyOfRange(last*width,(last+1)*width)
        val right=times.indices.first { times[it]>time }
        val left=right-1
        val blend=if(interpolation=="STEP") 0f else (time-times[left])/(times[right]-times[left])
        val a=values.copyOfRange(left*width,(left+1)*width)
        val b=values.copyOfRange(right*width,(right+1)*width)
        if(path!="rotation") return FloatArray(width){a[it]+(b[it]-a[it])*blend}
        return slerp(a,b,blend)
    }
    private fun slerp(a:FloatArray,b:FloatArray,t:Float):FloatArray {
        fun norm(v:FloatArray):FloatArray {val len=sqrt(v.sumOf{(it*it).toDouble()}).toFloat();require(len>1e-7f);return FloatArray(4){v[it]/len}}
        val p=norm(a);var q=norm(b)
        var dot=p.indices.sumOf{(p[it]*q[it]).toDouble()}.toFloat()
        if(dot<0f){q=FloatArray(4){-q[it]};dot=-dot}
        if(dot>.9995f) return norm(FloatArray(4){p[it]+t*(q[it]-p[it])})
        val theta=acos(dot.coerceIn(-1f,1f))
        val d=sin(theta)
        return norm(FloatArray(4){ (sin((1f-t)*theta)/d)*p[it]+(sin(t*theta)/d)*q[it] })
    }
}
data class ImportedAnimation(val name:String, val channels:List<AnimationChannel>) {
    init { require(channels.isNotEmpty()); require(channels.map { it.nodeIndex to it.path }.distinct().size==channels.size) }
    fun evaluate(time:Float, rest:List<JointTRS>):List<JointTRS> {
        val result=rest.toMutableList()
        for(c in channels) {
            require(c.nodeIndex in result.indices)
            val v=c.sample(time)
            val old=result[c.nodeIndex]
            result[c.nodeIndex]=when(c.path){
                "translation"->old.copy(translation=Vec3(v[0],v[1],v[2]))
                "scale"->old.copy(scale=Vec3(v[0],v[1],v[2]))
                else ->old.copy(rotation=Quaternion(v[0],v[1],v[2],v[3]).normalized())
            }
        }
        return result
    }
}
object GltfAnimationDecoder {
    private fun obj(v:Any?):Map<*,*> = v as? Map<*,*> ?: error("expected object")
    private fun arr(v:Any?):List<*> = v as? List<*> ?: error("expected array")
    private fun integer(v:Any?):Int {val n=v as? Number ?: error("expected integer");val i=n.toInt();require(n.toDouble()==i.toDouble());return i}
    private fun offset(v:Any?):Int = v?.let(::integer)?:0
    fun decode(bytes:ByteArray, animationIndex:Int=0):ImportedAnimation {
        val glb=GlbReader.read(bytes);val binary=requireNotNull(glb.binaryChunk)
        val root=obj(JsonSubset.parse(glb.jsonUtf8))
        val animations=arr(root["animations"]);val anim=obj(animations[animationIndex]);val samplers=arr(anim["samplers"])
        val channels=arr(anim["channels"]);require(channels.size in 1..256)
        val accessors=arr(root["accessors"]);val views=arr(root["bufferViews"])
        val nodes=arr(root["nodes"])
        fun floats(accessorIndex:Int,expectedType:String):FloatArray {
            val acc=obj(accessors[accessorIndex]);require(acc["type"]==expectedType && integer(acc["componentType"])==5126 && acc["sparse"]==null)
            val count=integer(acc["count"]);require(count in 1..50000)
            val components=when(expectedType){"SCALAR"->1;"VEC3"->3;"VEC4"->4;else->error("unsupported")}
            val width=components*4
            val view=obj(views[integer(acc["bufferView"])]);require(integer(view["buffer"])==0)
            val stride=offset(view["byteStride"]).let{if(it==0)width else it};require(stride>=width && stride%4==0)
            val accessorOffset=offset(acc["byteOffset"]);val viewOffset=offset(view["byteOffset"])
            val viewLength=integer(view["byteLength"])
            val required=accessorOffset.toLong()+(count-1).toLong()*stride+width
            require(viewOffset>=0 && accessorOffset>=0 && required<=viewLength && viewOffset.toLong()+required<=binary.size)
            val buffer=ByteBuffer.wrap(binary).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(count*components) {k->buffer.getFloat(viewOffset+accessorOffset+(k/components)*stride+(k%components)*4).also{require(it.isFinite())}}
        }
        val parsed=channels.map { raw ->
            val c=obj(raw);val sampler=obj(samplers[integer(c["sampler"])]);val target=obj(c["target"])
            val path=target["path"] as? String ?: error("no target path")
            require(path in setOf("translation","rotation","scale"))
            val node=integer(target["node"]);require(node in nodes.indices)
            val interpolation=sampler["interpolation"] as? String ?: "LINEAR"
            val times=floats(integer(sampler["input"]),"SCALAR")
            val values=floats(integer(sampler["output"]),if(path=="rotation")"VEC4" else "VEC3")
            AnimationChannel(node,path,interpolation,times,values)
        }
        return ImportedAnimation(anim["name"] as? String ?: "animation_$animationIndex",parsed)
    }
}

/** Model-space skin palettes use all ancestors, including non-joint glTF nodes. */
object AnimatedSkinPose {
    fun jointMatrices(skin:ImportedSkin, nodeParents:IntArray, locals:List<JointTRS>):List<Mat4> {
        require(nodeParents.size==locals.size)
        val state=IntArray(locals.size)
        val world=arrayOfNulls<Mat4>(locals.size)
        fun resolve(node:Int):Mat4 {
            require(node in locals.indices)
            require(state[node]!=1){"node cycle"}
            if(state[node]==2) return world[node]!!
            state[node]=1
            val parent=nodeParents[node]
            val result=if(parent<0) locals[node].matrix() else resolve(parent)*locals[node].matrix()
            world[node]=result;state[node]=2;return result
        }
        return skin.nodeIndices.map{resolve(it)}
    }
}
