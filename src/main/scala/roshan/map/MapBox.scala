package roshan.map

import akka.actor.{ActorLogging, Actor}
import roshan.buffer.Msg.CharacterAction
import roshan.protocols.CharacterChangesProtocol.CharacterChangeBroadcast
import roshan.protocols.CharacterProtocol._
import roshan.protocols.MapProtocol._
import roshan.model.Direction._
import roshan.{Loaderable, Mappable}
import roshan.{Server => _Server}

class MapBox(val mapX: Int = 0, val mapY: Int = 0, val Server: Mappable with Loaderable = _Server)
  extends Actor with ActorLogging with EventBox with CharacterHandler with MapInfo {

  def receive = SubUnsub orElse characterActions orElse HandleMapInfo orElse {

    case Walk(direction, speed) =>
      val (x: Int, y: Int) = grid.characterPosition(sender)
      val (newX, newY) = (x + to_XY(direction)._1, y + to_XY(direction)._2)
      moveOnOurMap(newX, newY) match {
        case _ if newX < 0 || newY < 0 =>

        case true if tile_map.isEmpty => // Do nothing if map isn't loaded yet

        case true if !grid.checkCharacterCollision(newX, newY) && !checkMapCollision(newX, newY) =>
          grid.move(sender, newX, newY)
          publishCharacterChange(id = char_id(sender), x = newX, y = newY, walk = true)

        case true => // Do nothing if there is a collision

        case false =>
          Server.mapBox(newX, newY) ! MoveCharacter(newX, newY, char_id(sender), sender)
      }

    case Say(say) =>
      publishCharacterChange(id = char_id(sender), say = say)

    case DirAction(action, rangeX, rangeY, direction, damage) =>
      val (x: Int, y: Int) = grid.characterPosition(sender)
      val (dirX: Int, dirY: Int) = to_XY(direction)
      // Virtually move character in direction for action so it doesn't hit itself
      val (charX, charY) = (x + dirX, y + dirY)
      // Find people in that area of attack
      for (x <- -rangeX to rangeX;
           y <- -rangeY to rangeY) {
        val (rotatedX, rotatedY) = rotate(direction, charX, charY, x, y)
        Server.mapBox(rotatedX, rotatedY) ! DoAction((rotatedX, rotatedY), action, damage)
      }

      publishCharacterChange(id = char_id(sender), action = action, direction = direction)

    case DoAction(xy, action, damage) =>
      grid getAt xy foreach (_ ! Hit(damage, action))

    case Action(action, direction, _) =>
      publishCharacterChange(id = char_id(sender), action = action, direction = direction)

    case AreaMessage(x, y, x2, y2, AddEffect(collidable, effect)) =>
      for (x <- x to x2; y <- y to y2)
        grid.add(effect, x, y, collidable)

    case SendAllUsers(x, y) =>
      grid forAllCharacters { char =>
        val ((x, y), id) = (grid.characterPosition(char), char_id(char).underlying)
        sender ! CharacterChangeBroadcast(
          CharacterAction.newBuilder().setId(id).setX(x).setY(y).build()
        )
      }

    case msg: Any =>
      log info "Map received unknown message %s from %s".format(msg.toString, sender.toString())
  }
}