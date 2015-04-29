package roshan.map

import akka.actor.{Actor, ActorRef}
import roshan.protocols.MapProtocol.{AddCharacter, RemoveCharacter, MoveCharacter, CharacterId}
import scala.collection.mutable
import roshan.protocols.CharacterProtocol.Moved
import roshan.Loaderable

trait CharacterHandler extends Actor with EventBox with MapInfo {
  var char_id = mutable.HashMap[ActorRef, CharacterId]()
  val Server: Loaderable

  def characterActions: Receive = {
    case MoveCharacter(x, y, id, character) =>
      if (!grid.checkCharacterCollision(x, y) && !checkMapCollision(x, y)) {
        grid.add(character, x, y)
        char_id += (character -> id)

        sender tell(RemoveCharacter(Some(x, y)), character)
        character ! Moved(x, y)
        publishCharacterChange(id = id, x = x, y = y, walk = true)
      }

    case AddCharacter(id, x, y) =>
      grid.add(sender, x, y)
      char_id += (sender -> new CharacterId(id))
      publishCharacterChange(id = char_id(sender), x = x, y = y)

    case RemoveCharacter(newXY) =>
      if (!newXY.isDefined)
        publishCharacterChange(char_id(sender), 0, 0, isGone = true)
      else newXY foreach {
        // If moving across channels post to the old place as well
        xy: (Int, Int) => publishCharacterChange(id = char_id(sender), x = xy._1, y = xy._2)
      }
      grid remove sender
      char_id -= sender
  }
}
