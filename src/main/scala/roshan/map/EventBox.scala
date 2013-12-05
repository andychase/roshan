package roshan.map

import akka.actor.ActorRef
import roshan.protocols.MapProtocol.CharacterId
import roshan.buffer.Msg.{CharacterAction, ACTION}
import roshan.model.Direction._
import roshan.protocols.CharacterChangesProtocol._

/** Event Box is a event pub/sub system for map boxes. */
trait EventBox {
  /** This is a list of characters that are subscribes to actions here */
  var subscribers = Set[ActorRef]()
  def publish(event: CharacterChangeBroadcast) { subscribers foreach ( _ ! event) }
  def subscribe(subscriber:ActorRef) { subscribers += subscriber }
  def unsubscribe(subscriber:ActorRef) { subscribers -= subscriber }

  def publishCharacterChange (
       id:CharacterId,
       x:Int = -1,
       y:Int = -1,
       action:ACTION = null,
       direction:Direction = null,
       walk:Boolean = false,
       isGone:Boolean = false,
       say:String = null
  ) {
    val msg = CharacterAction.newBuilder()
    msg.setId(id.underlying)

    if (isGone)
      msg.setGone(true)
    if (action != null)
      msg.setAction(action)
    if (x != -1 && y != -1)
      msg.setX(x).setY(y)
    if (walk)
      msg.setWalk(true)
    if (direction != null)
      msg.setDirection(direction.id)
    if (say != null)
      msg.setSay(say)

    publish(CharacterChangeBroadcast(msg.build()))
  }
}
