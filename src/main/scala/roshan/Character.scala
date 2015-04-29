package roshan

import akka.actor._
import buffer.Msg.ACTION._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import roshan.buffer.Msg.ACTION
import roshan.item.ItemFactory
import roshan.buffer.Msg.ITEM_ATTR._
import roshan.protocols.CharacterProtocol.Tick
import roshan.protocols.MapProtocol.RemoveCharacter
import roshan.protocols.CharacterProtocol.Hit
import roshan.protocols.MapProtocol.Walk
import roshan.protocols.CharacterProtocol.Moved
import roshan.protocols.CharacterProtocol.Action
import roshan.protocols.MapProtocol.AddCharacter
import roshan.db.dbCharacter
import roshan.protocols.MapProtocol.Say
import roshan.protocols.CharacterProtocol.ReceiveItem
import roshan.protocols.CharacterProtocol.SaveNow

class Character(client:ActorRef = null, Server:Mappable = Server, character:dbCharacter = Useful.defaultCharacter)
                                                                                       extends Actor with ActorLogging {
  val scheduler = context.system.scheduler
  var tick_scheduler:Cancellable = scheduler.schedule(0 milliseconds, 200 milliseconds, self, Tick)

  var my:dbCharacter = character
  var tokens:List[(ACTION, Int)] = Nil
  var maxHealth = 30
  var ticks = 0

  var map = Server.mapBox(my.x, my.y)
  // Add char to map if id is present
  my.id foreach (map ! AddCharacter(_, my.x, my.y) )

  // Monkey Patching sword back in for now
  my.items = List(List(SWORDATTR, BASIC_DMG))
  // Don't need this right meow
  tick_scheduler.cancel()

  def setFacing():Receive = {
    case Action(action, direction, say) if my.direction != direction =>
      my.direction = direction
      receive(Action(action, direction, say))
  }

  def receive = setFacing orElse {
    case Action(action, direction, say) => action match {
      case SWORD if my.items.nonEmpty =>
        val replacement = ItemFactory use(my.items.head, map, this)
        // Replace into items
        my.items = replacement map (List(_)) getOrElse Nil ::: my.items.splitAt(1)._2
      case SAY =>
        map ! Say(say)
      case WALK =>
        map ! Walk(direction, my.speed)
      case HURT =>
    }

    case ReceiveItem(item) =>
      my.items = item :: my.items

    case Hit(damage, action) =>
      my.health -= damage
      map ! Action(ACTION.HURT, my.direction)
      if (my.health < 1)
        self ! SaveNow

    case Tick =>
      ticks += 1
      tokens = tokens filter {token =>
        self ! token._1
        ticks > token._2
      }

    case Moved(x, y) =>
      map = Server.mapBox(x, y)

    case SaveNow =>
      map ! RemoveCharacter()
      self ! PoisonPill

    case _ =>
      log info "Character received unknown message"
  }
}
