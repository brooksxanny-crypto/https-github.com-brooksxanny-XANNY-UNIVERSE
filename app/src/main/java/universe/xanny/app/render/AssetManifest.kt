package universe.xanny.app.render

/** Asset manifest validation only. No claim of glTF decoding or rig loading. */
data class AssetDescriptor(
    val assetId: String, val path: String, val sha256: String,
    val type: AssetType, val byteLength: Long
)
enum class AssetType { GLB, GLTF, TEXTURE, ANIMATION }
class AssetManifest(assets: List<AssetDescriptor>) {
    val records: Map<String, AssetDescriptor>
    init {
        require(assets.map { it.assetId }.distinct().size == assets.size)
        assets.forEach { a ->
            require(a.assetId.matches(Regex("[a-zA-Z0-9_-]{1,80}")))
            require(a.path.isNotBlank() && !a.path.startsWith('/') &&
                !a.path.startsWith('\\') && !a.path.contains('\\') &&
                a.path.split('/').none { it == ".." || it == "." || it.isBlank() })
            require(a.sha256.matches(Regex("[a-fA-F0-9]{64}")))
            require(a.byteLength >= 0)
            val extension=a.path.substringAfterLast('.', "").lowercase()
            val allowed=when(a.type) {
                AssetType.GLB -> setOf("glb")
                AssetType.GLTF -> setOf("gltf")
                AssetType.TEXTURE -> setOf("png", "jpg", "jpeg", "ktx2")
                AssetType.ANIMATION -> setOf("glb", "gltf")
            }
            require(extension in allowed)
        }
        records=assets.associateBy { it.assetId }
    }
}
