package roshan
import akka.actor.{ActorLogging, ActorRef, Actor}
import buffer.Msg.{LAYERS, MapData, CharacterAction}
import collection.immutable.HashMap
import roshan.protocols.CharacterChangesProtocol.{Unsubscribe, Subscribe, CharacterChangeBroadcast}
import roshan.protocols.CharacterProtocol._
import roshan.protocols.LoaderProtocol._
import roshan.protocols.MapProtocol._
import roshan.model.Grid
import roshan.model.Direction._
import roshan.Useful._

class Map(val mapX:Int = 0, val mapY:Int = 0, Server:Mappable with Loaderable = Server)
                                                            extends Actor with ActorLogging with EventBox {
  var grid = new Grid(tilesPerMap, tilesPerMap, mapX, mapY)
  var char_id = HashMap[ActorRef, CharacterId]()
  var tile_map:Option[MapData] = None
  /** If the map isn't loaded yet, queue up walk messages here */
  var waitingWalkMsg = List[(Walk, ActorRef)]()

  /** If we crash, resend map */
  override def postRestart(cause: Throwable) {
    Server.sendMap(mapX, mapY, self)
  }

  def checkMapCollision(x:Int, y:Int):Boolean = {
    val tileXY = Useful.getTileRelativeToMapSection(x, y)
    (tile_map map (_ getLayer LAYERS.COLLISION_VALUE getTile tileXY) getOrElse 0) != 0
  }

  def moveOnOurMap(x:Int, y:Int):Boolean =
    mapSection(x, y) == mapSection(mapX, mapY)

  def receive = {
    case Subscribe() =>
      subscribe(sender)

    case Unsubscribe =>
      unsubscribe(sender)

    case SendMap(x, y, recipient) =>
      if (tile_map.isDefined) recipient ! ReceiveMap(x, y, tile_map.get)
      else Server.sendMap(x, y, recipient)

    case ReceiveMap(x, y, mapData) =>
      tile_map = Some(mapData)
      waitingWalkMsg.foreach({i:(Walk, ActorRef) => self.tell(i._1, i._2)})

    case AddCharacter(id, x, y) =>
      grid.add(sender, x, y)
      char_id += (sender -> new CharacterId(id))
      publishCharacterChange(id= char_id(sender), x= x, y= y)

    case RemoveCharacter(newXY) =>
      if (!newXY.isDefined)
        publishCharacterChange(char_id(sender), 0, 0, isGone = true)
      else newXY foreach {
        // If moving across channels post to the old place as well
        xy:(Int, Int) => publishCharacterChange(id = char_id(sender), x = xy._1, y = xy._2)
      }
      grid remove sender
      char_id -= sender

    case Walk(direction, speed) =>
      val (x:Int, y:Int) = grid.characterPosition(sender)
      val (newX, newY) = (x + to_XY(direction)._1, y + to_XY(direction)._2)
      moveOnOurMap(newX, newY) match {
        case _ if newX < 0 || newY < 0 =>

        case true if tile_map.isEmpty =>
          waitingWalkMsg = (Walk(direction, speed), sender) :: waitingWalkMsg

        case true if !grid.checkCharacterCollision(newX, newY) && !checkMapCollision(newX, newY) =>
          grid.move(sender, newX, newY)
          publishCharacterChange(id= char_id(sender), x= newX, y= newY, walk= true)

        case true => // Do nothing if there is a collision

        case false =>
          Server.mapBox(newX, newY) ! MoveCharacter(newX, newY, char_id(sender), sender)
      }

    case MoveCharacter(x, y, id, character) =>
      if (!grid.checkCharacterCollision(x, y) && !checkMapCollision(x, y)) {
        grid.add(character, x, y)
        char_id += (character -> id)

        sender tell (RemoveCharacter(Some(x, y)), character)
        character ! Moved(x, y)
        publishCharacterChange(id= id, x= x, y= y, walk= true)
      }

    case Say(say) =>
      publishCharacterChange(id= char_id(sender), say= say)

    case DirAction(action, rangeX, rangeY, direction, damage) =>
      val (x:Int, y:Int) = grid.characterPosition(sender)
      val (dirX:Int, dirY:Int) = to_XY(direction)
      // Virtually move character in direction for action so it doesn't hit itself
      val (charX, charY) = (x + dirX, y + dirY)
      // Find people in that area of attack
      for (x <- -rangeX to rangeX;
           y <- -rangeY to rangeY) {
        val (rotatedX, rotatedY) = rotate(direction, charX, charY, x, y)
        Server.mapBox(rotatedX, rotatedY) ! DoAction((rotatedX, rotatedY), action, damage)
      }

      publishCharacterChange (id= char_id(sender), action= action, direction= direction)

    case DoAction(xy, action, damage) =>
      grid getAt xy foreach (_ ! Hit(damage, action))

    case Action(action, direction, _) =>
      publishCharacterChange (id= char_id(sender), action= action, direction= direction)

    case AreaMessage(x, y, x2, y2, AddEffect(collidable, effect)) =>
      for (x <- x to x2; y <- y to y2)
        grid.add(effect, x, y, collidable)

    case SendAllUsers(x, y) =>
      grid forAllCharacters {char =>
        val((x,y), id) = (grid.characterPosition(char), char_id(char).underlying)
        sender ! CharacterChangeBroadcast(
          CharacterAction.newBuilder().setId(id).setX(x).setY(y).build()
        )
      }

    case SaveCharacter(char) =>
      val (x, y) = grid.characterPosition(sender)
      Server.saveChar(char.copy(x= x, y= y), sender)

    case msg:Any =>
      log info "Map received unknown message %s from %s".format(msg.toString, sender.toString())
  }
}