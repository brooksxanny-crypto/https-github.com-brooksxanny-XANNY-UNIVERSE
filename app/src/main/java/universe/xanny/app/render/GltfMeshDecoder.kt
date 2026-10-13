package universe.xanny.app.render

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Strict subset: GLB2, one indexed TRIANGLES primitive, tightly packed or strided float VEC3 positions,
 * UNSIGNED_BYTE/SHORT indices. Not a general glTF renderer, normals/skins/materials remain unsupported. */
data class ImportedTriangleMesh(
    val positions: FloatArray, val indices: ShortArray,
    val normals: FloatArray? = null, val material: ImportedMaterial = ImportedMaterial(),
    val skinWeights: SkinWeights? = null
)
data class ImportedMaterial(
    val baseColor: FloatArray = floatArrayOf(1f,1f,1f,1f),
    val metallic: Float = 1f, val roughness: Float = 1f
)

object GltfMeshDecoder {
    fun decode(bytes: ByteArray): ImportedTriangleMesh = decodeAll(bytes).first()
    fun decodeAll(bytes: ByteArray): List<ImportedTriangleMesh> {
        val glb = GlbReader.read(bytes)
        val bin = requireNotNull(glb.binaryChunk) { "GLB BIN required" }
        val root = JsonSubset.parse(glb.jsonUtf8) as? Map<*, *> ?: error("Invalid glTF root")
        val asset = obj(root["asset"])
        require(asset["version"] == "2.0")
        val meshes = arr(root["meshes"])
        require(meshes.isNotEmpty())
        val primitiveSpecs = meshes.flatMap { mesh -> arr(obj(mesh)["primitives"]) }
        require(primitiveSpecs.size in 1..128)
        val materials = (root["materials"] as? List<*>) ?: emptyList<Any>()
        val accessors = arr(root["accessors"])
        val views = arr(root["bufferViews"])
        val buffers = arr(root["buffers"])
        require(buffers.size == 1 && num(obj(buffers[0])["byteLength"]) <= bin.size)
        return primitiveSpecs.map { primitiveSpec ->
        val primitive = obj(primitiveSpec)
        require((primitive["mode"] as? Number)?.toInt() in listOf(null, 4)) { "TRIANGLES only" }
        val attrs = obj(primitive["attributes"])
        val positionId = num(attrs["POSITION"])
        val posAcc = obj(accessors[positionId])
        require(posAcc["type"] == "VEC3" && num(posAcc["componentType"]) == 5126)
        require(posAcc["sparse"] == null && posAcc["normalized"] != true)
        val count = num(posAcc["count"])
        require(count in 3..65535)
        val posView = obj(views[num(posAcc["bufferView"])])
        val posData = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN)
        val posStride = (posView["byteStride"] as? Number)?.toInt() ?: 12
        require(posStride >= 12 && posStride % 4 == 0)
        val posStart = checkedBase(posView, posAcc, count, posStride, 12, bin.size)
        val positions = FloatArray(count * 3)
        for (i in 0 until count) for (j in 0..2) {
            val v = posData.getFloat(posStart + i*posStride + j*4)
            require(v.isFinite())
            positions[i*3+j] = v
        }
        val normals = attrs["NORMAL"]?.let { normalId ->
            val normAcc = obj(accessors[num(normalId)])
            require(normAcc["type"]=="VEC3" && num(normAcc["componentType"])==5126)
            require(normAcc["sparse"]==null && normAcc["normalized"]!=true && num(normAcc["count"])==count)
            val normView=obj(views[num(normAcc["bufferView"])])
            val stride=(normView["byteStride"] as? Number)?.toInt()?:12
            require(stride>=12 && stride%4==0)
            val start=checkedBase(normView,normAcc,count,stride,12,bin.size)
            FloatArray(count*3) { k -> posData.getFloat(start+(k/3)*stride+(k%3)*4).also { require(it.isFinite()) } }
        }
        val material = primitive["material"]?.let { materialId ->
            val info=obj(materials[num(materialId)])
            val pbr=(info["pbrMetallicRoughness"] as? Map<*,*>) ?: emptyMap<Any,Any>()
            val rgba=((pbr["baseColorFactor"] as? List<*>) ?: listOf(1.0,1.0,1.0,1.0)).map { (it as Number).toFloat() }.toFloatArray()
            require(rgba.size==4 && rgba.all { it.isFinite() && it in 0f..1f })
            val metal=(pbr["metallicFactor"] as? Number)?.toFloat()?:1f
            val rough=(pbr["roughnessFactor"] as? Number)?.toFloat()?:1f
            require(metal.isFinite() && metal in 0f..1f && rough.isFinite() && rough in 0f..1f)
            ImportedMaterial(rgba,metal,rough)
        } ?: ImportedMaterial()
        val indexId = num(primitive["indices"])
        val ixAcc = obj(accessors[indexId])
        require(ixAcc["type"] == "SCALAR" && ixAcc["sparse"] == null)
        val type = num(ixAcc["componentType"])
        val unit = when(type) { 5121 -> 1; 5123 -> 2; else -> error("Only UBYTE/USHORT indices supported") }
        val n = num(ixAcc["count"])
        require(n in 3..1000000 && n % 3 == 0)
        val ixView = obj(views[num(ixAcc["bufferView"])])
        val stride = (ixView["byteStride"] as? Number)?.toInt() ?: unit
        require(stride >= unit && stride % unit == 0)
        val ixStart = checkedBase(ixView, ixAcc, n, stride, unit, bin.size)
        val indices = ShortArray(n) { i ->
            val offset = ixStart + i * stride
            val value = if (unit == 1) bin[offset].toInt() and 255 else posData.getShort(offset).toInt() and 65535
            require(value < count)
            value.toShort()
        }
        val hasJoints = attrs.containsKey("JOINTS_0")
        val hasWeights = attrs.containsKey("WEIGHTS_0")
        require(hasJoints == hasWeights) { "JOINTS_0 and WEIGHTS_0 must occur together" }
        val influences = if (hasJoints) {
            val ja = obj(accessors[num(attrs["JOINTS_0"])])
            val wa = obj(accessors[num(attrs["WEIGHTS_0"])])
            require(ja["type"] == "VEC4" && wa["type"] == "VEC4")
            require(num(ja["count"]) == count && num(wa["count"]) == count)
            val jt = num(ja["componentType"])
            require(jt == 5121 || jt == 5123)
            val wt = num(wa["componentType"])
            require(wt in listOf(5126,5121,5123))
            require(ja["sparse"] == null && wa["sparse"] == null && ja["normalized"] != true)
            require(wt == 5126 || wa["normalized"] == true)
            val jointUnit = if (jt == 5121) 1 else 2
            val weightUnit = when(wt) { 5126 -> 4; 5123 -> 2; else -> 1 }
            val jv = obj(views[num(ja["bufferView"])])
            val wv = obj(views[num(wa["bufferView"])])
            val js = (jv["byteStride"] as? Number)?.toInt() ?: jointUnit*4
            val ws = (wv["byteStride"] as? Number)?.toInt() ?: weightUnit*4
            require(js >= jointUnit*4 && ws >= weightUnit*4 && js%jointUnit == 0 && ws%weightUnit == 0)
            val jb = checkedBase(jv,ja,count,js,jointUnit*4,bin.size)
            val wb = checkedBase(wv,wa,count,ws,weightUnit*4,bin.size)
            val joints = IntArray(count*4)
            val weights = FloatArray(count*4)
            for (v in 0 until count) for (slot in 0..3) {
                val ji = jb+v*js+slot*jointUnit
                val wi = wb+v*ws+slot*weightUnit
                joints[v*4+slot] = if (jt==5121) bin[ji].toInt() and 255 else posData.getShort(ji).toInt() and 65535
                weights[v*4+slot] = when(wt) {
                    5126 -> posData.getFloat(wi)
                    5123 -> (posData.getShort(wi).toInt() and 65535)/65535f
                    else -> (bin[wi].toInt() and 255)/255f
                }
            }
            SkinWeights(joints,weights,count)
        } else null
        ImportedTriangleMesh(positions, indices, normals, material, influences)
        }
    }
    /** Parses skin joint node lists and inverse bind accessor, preserving glTF skin ordering. */
    fun decodeSkin(bytes: ByteArray, skinIndex: Int = 0): ImportedSkin {
        val glb=GlbReader.read(bytes)
        val bin=requireNotNull(glb.binaryChunk)
        val root=obj(JsonSubset.parse(glb.jsonUtf8))
        val skins=arr(root["skins"])
        val skin=obj(skins[skinIndex])
        val jointNodes=arr(skin["joints"]).map { num(it) }
        require(jointNodes.isNotEmpty() && jointNodes.size<=256 && jointNodes.distinct().size==jointNodes.size)
        val nodes=arr(root["nodes"])
        require(jointNodes.all { it in nodes.indices })
        val parents=IntArray(nodes.size){-1}
        nodes.forEachIndexed { p,item ->
            (obj(item)["children"] as? List<*>)?.forEach { c ->
                val child=num(c); require(child in nodes.indices && parents[child]==-1)
                parents[child]=p
            }
        }
        val inverse=if (skin["inverseBindMatrices"]==null) List(jointNodes.size){Mat4.IDENTITY} else {
            val accessors=arr(root["accessors"]);val views=arr(root["bufferViews"])
            val acc=obj(accessors[num(skin["inverseBindMatrices"])])
            require(acc["type"]=="MAT4" && num(acc["componentType"])==5126 && num(acc["count"])==jointNodes.size)
            require(acc["sparse"]==null)
            val view=obj(views[num(acc["bufferView"])])
            val stride=(view["byteStride"] as? Number)?.toInt()?:64
            require(stride>=64 && stride%4==0)
            val base=checkedBase(view,acc,jointNodes.size,stride,64,bin.size)
            val data=ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN)
            List(jointNodes.size){j -> Mat4(FloatArray(16){k-> data.getFloat(base+j*stride+k*4)}) }
        }
        val nodeToJoint=jointNodes.withIndex().associate { it.value to it.index }
        val parentJoints=jointNodes.map { node ->
            var cursor=parents[node]; var parent:Int?=null; var limit=0
            while(cursor!=-1) {
                require(++limit<=nodes.size) { "Node graph cycle" }
                if (nodeToJoint.containsKey(cursor)) { parent=nodeToJoint[cursor]; break }
                cursor=parents[cursor]
            }
            parent
        }
        return ImportedSkin(jointNodes,inverse,parentJoints)
    }
    private fun checkedBase(view: Map<*, *>, accessor: Map<*, *>, count: Int, stride: Int, width: Int, size: Int): Int {
        require(num(view["buffer"]) == 0)
        val base = opt(view["byteOffset"]) + opt(accessor["byteOffset"])
        val length = num(view["byteLength"])
        val endInside = opt(accessor["byteOffset"]).toLong() + (count-1).toLong()*stride + width
        require(base >= 0 && length >= 0 && endInside <= length && base.toLong()+(count-1).toLong()*stride+width <= size)
        return base
    }
    private fun opt(v: Any?): Int = (v as? Number)?.toInt() ?: 0
    private fun num(v: Any?): Int = (v as? Number)?.toInt() ?: error("Missing integer")
    private fun obj(v: Any?): Map<*, *> = v as? Map<*, *> ?: error("Expected object")
    private fun arr(v: Any?): List<*> = v as? List<*> ?: error("Expected array")
}

/** Bounded no-dependency JSON subset parser for validated GLB metadata, rejects duplicate keys. */
internal class JsonSubset(private val source: String) {
    private var i = 0
    companion object { fun parse(s: String): Any? { require(s.length <= 2_000_000); return JsonSubset(s).run() } }
    private fun run(): Any? { val value = read(0); ws(); require(i == source.length); return value }
    private fun ws() { while (i < source.length && source[i].isWhitespace()) i++ }
    private fun read(depth: Int): Any? {
        require(depth < 40); ws(); require(i < source.length)
        return when(source[i]) {
            '{' -> { i++; ws(); val m=linkedMapOf<String,Any?>(); if (peek('}')) {i++;m} else {
                do { ws(); val k=string(); ws(); expect(':'); require(!m.containsKey(k)); m[k]=read(depth+1); ws(); if(peek('}')) {i++;break}; expect(',') } while(true); m }
            }
            '[' -> { i++; ws(); val list=ArrayList<Any?>(); if(peek(']')) {i++;list} else {
                do { require(list.size<1000000);list.add(read(depth+1));ws();if(peek(']')) {i++;break};expect(',') } while(true); list }
            }
            '"' -> string()
            't' -> { literal("true");true }
            'f' -> { literal("false");false }
            'n' -> { literal("null");null }
            else -> number()
        }
    }
    private fun peek(c: Char): Boolean = i<source.length && source[i]==c
    private fun expect(c:Char) { ws();require(peek(c));i++ }
    private fun literal(v:String) { require(source.startsWith(v,i));i+=v.length }
    private fun string(): String {
        expect('"');val out=StringBuilder()
        while(i<source.length) {
            val c=source[i++]; if(c=='"') return out.toString()
            if(c=='\\') { require(i<source.length); val e=source[i++];out.append(when(e) {
                '"'->'"';'\\'->'\\';'/'->'/';'b'->'\b';'f'->'\u000c';'n'->'\n';'r'->'\r';'t'->'\t';'u'->{require(i+4<=source.length);val n=source.substring(i,i+4).toInt(16);i+=4;n.toChar()};else->error("Bad escape")})
            } else {require(c.code>=32);out.append(c)}
        };error("Unclosed string")
    }
    private fun number(): Number {
        val start=i
        while(i<source.length && source[i] in "-+0123456789.eE") i++
        require(i>start)
        val raw=source.substring(start,i)
        require(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?").matches(raw))
        return raw.toDouble().also { require(it.isFinite()) }
    }
}
