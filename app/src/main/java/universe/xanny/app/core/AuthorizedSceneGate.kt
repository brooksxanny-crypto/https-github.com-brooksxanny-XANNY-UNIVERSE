package universe.xanny.app.core

/** Policy-only gate. It never claims a scene mutation has been completed. */
class AuthorizedSceneGate(private val registeredTargets: Set<SceneObjectRef>) {
    fun validate(intent: ActionIntent, permission: ActorPermission): SceneMutationResult {
        if (!permission.granted || permission.principalId != intent.actorId || permission.capability != intent.action)
            return SceneMutationResult.Rejected("Authorization unavailable")
        if (intent.target !in registeredTargets || intent.target.version < 0L)
            return SceneMutationResult.Rejected("Target not registered")
        return SceneMutationResult.Rejected("No authoritative scene mutation executor attached")
    }
}
