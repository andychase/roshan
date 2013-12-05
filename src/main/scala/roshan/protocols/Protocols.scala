package roshan.protocols
import roshan.buffer.Msg.{ITEM_ATTR, CharacterAction, MapData, ACTION}
import akka.actor.ActorRef
import roshan.db.dbCharacter
import akka.util.ByteString
import roshan.model.Direction.Direction

object ClientProtocol {
  case class Cleanup()
  case class ReceiveMessage(Message: ByteString)
  case class SendWorldChange()
}

object CharacterProtocol {
  type Inventory  = List[List[ITEM_ATTR]]
  type Item       =      List[ITEM_ATTR]

  case class Action(action:ACTION, direction:Direction, say:String = "")
  case class Hit(damage:Int, action:ACTION)
  case class Tick()
  case class SaveNow()
  case class ReceiveItem(item:Item)
  case class Heal(amount:Int)
  case class Moved(x:Int, y:Int)
}

object MapProtocol {
  case class SendAllUsers(x:Int, y:Int)
  case class AddCharacter(id:Int, x:Int, y:Int)
  case class MoveCharacter(x:Int, y:Int, id:CharacterId, whom:ActorRef)
  case class RemoveCharacter(newXY:Option[(Int, Int)] = None)
  case class Walk(direction:Direction, speed:Int)
  case class Say(say:String)
  case class DirAction(action:ACTION, rangeX:Int, rangeY:Int, direction:Direction, damage:Int)
  case class DoAction(xy:(Int, Int), action:ACTION, damage:Int)

  case class AreaMessage(x:Int, y:Int, x2:Int, y2:Int, message:Any)
  case class AddEffect(collidable:Boolean, effect:ActorRef)
  case class RemoveEffect(effect:ActorRef)

  class CharacterId(val underlying: Int) extends AnyVal
}

object LoaderProtocol {
  case class SendMaps()
  case class SendItems(toWhom:ActorRef)
  case class SendMap(x: Int, y: Int, toWhom: ActorRef)
  case class ReceiveMap(x:Int, y:Int, Map: MapData)

  case class SaveCharacter(char:dbCharacter)
  /** New Character (if None) */
  case class LoadCharacter(char:dbCharacter)
}

object CharacterChangesProtocol {
  case class CharacterChangeBroadcast(change: CharacterAction)
  case class Subscribe()
  case class Unsubscribe()
}

object ItemProtocol {
  case class UseItem()
}

object LoginProtocol {
  case class Register()
}