package universe.xanny.app.core

/** Models contracts; does not imply a functioning renderer, AI, music engine or backend. */
data class SceneObjectRef(val sceneId: String, val objectId: String, val version: Long)
data class ActorPermission(val principalId: String, val capability: String, val granted: Boolean)
data class ActionIntent(val actorId: String, val target: SceneObjectRef, val action: String)
sealed interface SceneMutationResult {
    data class Committed(val authoritativeRevision: Long): SceneMutationResult
    data class Rejected(val reason: String): SceneMutationResult
}
interface SceneAuthority {
    suspend fun execute(intent: ActionIntent, permission: ActorPermission): SceneMutationResult
}
interface SpatialWorldRenderer { val state: DeliveryState }
interface CharacterRigRuntime { val state: DeliveryState }
interface VolumetricH3Renderer { val state: DeliveryState }
interface ReflectiveChromeRuntime { val state: DeliveryState }
interface AuthorizedMusicRuntime { val state: DeliveryState }
interface TogetherCommunicationRuntime { val state: DeliveryState }
interface EvidenceProvider { fun evidenceFor(requirementId: String): String? }
