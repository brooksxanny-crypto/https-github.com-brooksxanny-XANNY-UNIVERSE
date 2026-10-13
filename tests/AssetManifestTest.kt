import universe.xanny.app.render.*
fun main() {
    val digest="a".repeat(64)
    val good=AssetManifest(listOf(AssetDescriptor("xanny_model","models/xanny.glb",digest,AssetType.GLB,42)))
    check(good.records.size==1)
    for (path in listOf("../secret.glb", "/private/a.glb", "a/../bad.glb", "a\\bad.glb", "a//bad.glb")) {
        var refused=false
        try {AssetManifest(listOf(AssetDescriptor("unsafe",path,digest,AssetType.GLB,12)))} catch (_:IllegalArgumentException){refused=true}
        check(refused) { "unsafe path accepted: $path" }
    }
    var refused=false
    try {AssetManifest(listOf(AssetDescriptor("x","a.glb","not-a-hash",AssetType.GLB,1)))} catch (_:IllegalArgumentException){refused=true}
    check(refused)
    println("PASS: manifest schema and path traversal validation")
}
