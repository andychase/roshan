package roshan
import akka.actor._
import akka.actor.IO.ReadHandle
import akka.event.Logging
import akka.util.ByteString
import buffer.Msg._
import com.google.protobuf.InvalidProtocolBufferException
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import roshan.protocols.MapProtocol._
import roshan.model.Direction
import roshan.protocols.LoaderProtocol._
import roshan.protocols.CharacterChangesProtocol._
import roshan.protocols.CharacterProtocol._
import roshan.protocols.ClientProtocol._
import roshan.protocols.LoginProtocol.Register

class Client(handle:ReadHandle) extends Actor {
  val log = Logging(context.system, this)
  var worldChange = WorldChange.newBuilder()
  val scheduler = context.system.scheduler
  var scheduled:Boolean = false
  val maxMessageSendInterval = 350
  var my_character_id:Option[CharacterId] = None
  var my_subscriptions:List[(Int, Int)] = Nil
  var my_current_quad = -1
  var hero:ActorRef = null

  context.become(waitingForCharacter)
  Server.login ! Register()

  def quadSubscriptions(quadrant:Int):List[(Int, Int)] =
    // Based on my quad, which relative subscriptions do I need?
    // Quadrant here means which part of the map section am I standing on
    // 0 is top left, 3 is bottom-right
    quadrant match {
      case 0 => (0, 0) :: (-1, -1) :: (-1,  0) :: (0, -1) :: Nil
      case 1 => (0, 0) :: ( 0, -1) :: ( 1, -1) :: (1,  0) :: Nil
      case 2 => (0, 0) :: (-1,  0) :: (-1,  1) :: (0,  1) :: Nil
      case 3 => (0, 0) :: ( 1,  0) :: ( 1,  1) :: (0,  1) :: Nil
    }

  def updateSubscriptions(x:Int, y:Int) {
    // Figure out which Quad i'm in
    val (mapBoxX, mapBoxY) = Useful.mapSection(x, y)
    val (relativeX, relativeY) = (x - mapBoxX, y - mapBoxY)
    val quad_x = if (relativeX < Useful.tilesPerMap / 2) 0 else 1
    val quad_y = if (relativeY < Useful.tilesPerMap / 2) 0 else 2
    val quad = quad_x + quad_y
    if (quad != my_current_quad) {
      my_current_quad = quad
      val old_subscriptions = my_subscriptions
      my_subscriptions = Nil
      // Build subscription addresses
      for (relativeSubscriptionNeeded:(Int, Int) <- quadSubscriptions(quad)) {
        val subscription_address = (
           ((mapBoxX / Useful.tilesPerMap) + relativeSubscriptionNeeded._1) * Useful.tilesPerMap,
           ((mapBoxY / Useful.tilesPerMap) + relativeSubscriptionNeeded._2) * Useful.tilesPerMap
          )
        my_subscriptions = subscription_address :: my_subscriptions
      }
      // Subscribe to the new channels
      for (subscription <- my_subscriptions diff old_subscriptions) {
        Server.mapBox(subscription) ! Subscribe()
        Server.mapBox(subscription) ! SendAllUsers(subscription _1, subscription _2)
        Server.mapBox(subscription) ! SendMap(subscription _1, subscription _2, self)
      }
      // Unsubscribe from the ones we don't need anymore and send the clear map signal
      for (subscription <- old_subscriptions diff my_subscriptions) {
        Server.mapBox(subscription) ! Unsubscribe()
        worldChange addMapChange MapChange.newBuilder()
          .setChange(CHANGE.Clear)
          .setMapBoxX(subscription _1)
          .setMapBoxY(subscription _2)
      }
    }
  }

  def sendOrSchedule() {
      self ! SendWorldChange
  }

  def translateMessage(raw: ByteString) {
    val msg = ClientChange parseDelimitedFrom new ByteArrayInputStream(raw.toArray)
    hero ! Action(msg.getAction, Direction(msg.getDirection), msg.getSay)
  }

  def waitingForCharacter:Receive = {
    case LoadCharacter(char) =>
      hero = context.system.actorOf(Props(new Character(client = self, character = char)))
      my_character_id = Option(new CharacterId(char.id.get))
      updateSubscriptions(char.x, char.y)

      worldChange addCharacterActions
        CharacterAction.newBuilder()
          .setIsYou(true)
          .setId(my_character_id.get.underlying)
          .setX(char.x)
          .setY(char.y)
          .build()
      sendOrSchedule()

      context.unbecome()
  }

  def receive = {
    case ReceiveMessage(message) =>
      try translateMessage(message)
      catch { case e: InvalidProtocolBufferException => }

    case CharacterChangeBroadcast(characterAction) =>
      if (characterAction.getId == my_character_id.get && characterAction.getWalk)
        updateSubscriptions(characterAction.getX, characterAction.getY)
      if (characterAction.getWalk
          && !my_subscriptions.contains(Useful.mapSection(characterAction.getX, characterAction.getY)))
          worldChange addCharacterActions
            CharacterAction
            .newBuilder()
            .setId(characterAction.getId)
            .setGone(true)
      else worldChange addCharacterActions characterAction
      sendOrSchedule()

    case ReceiveMap(x, y, mapData) =>
      worldChange addMapData mapData
      sendOrSchedule()

    case SendWorldChange =>
      // Send all the new data collected so far
      scheduled = false
      if (worldChange.getCharacterActionsCount > 0 || worldChange.getMapDataCount > 0) {
        val output_stream = new ByteArrayOutputStream()
        worldChange.build.writeDelimitedTo(output_stream)
        handle.asSocket write ByteString(output_stream.toByteArray)
        worldChange.clear()
      }

    case Cleanup() =>
      hero ! SaveNow
      hero ! PoisonPill
  }

  override def preRestart(cause: Throwable, msg: Option[Any]) {
      hero ! SaveNow
      hero ! PoisonPill
  }
}