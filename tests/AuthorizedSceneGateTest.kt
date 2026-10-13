import universe.xanny.app.core.*

fun main() {
  val t=SceneObjectRef("scene","orb",1)
  val gate=AuthorizedSceneGate(setOf(t))
  val req=ActionIntent("xanny",t,"rotate")
  check(gate.validate(req,ActorPermission("xanny","rotate",false)) is SceneMutationResult.Rejected)
  check(gate.validate(req,ActorPermission("tokyo","rotate",true)) is SceneMutationResult.Rejected)
  check(gate.validate(req.copy(target=t.copy(objectId="invalid")),ActorPermission("xanny","rotate",true)) is SceneMutationResult.Rejected)
  check(gate.validate(req,ActorPermission("xanny","rotate",true)) is SceneMutationResult.Rejected)
  println("PASS: denied, mismatched principal, unknown target, no executor = never claim committed")
}
