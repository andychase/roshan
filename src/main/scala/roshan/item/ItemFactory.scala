package roshan.item

import akka.actor.ActorRef
import roshan.Character
import roshan.buffer.Msg.ACTION._
import roshan.protocols.MapProtocol.DirAction
import roshan.protocols.CharacterProtocol.Item
import roshan.buffer.Msg.ITEM_ATTR._
import roshan.buffer.Msg.{ITEM_ATTR, ACTION}

/** This object takes Item 'Recipes' or 'Attributes' and creates an object and runs it */
object ItemFactory {
  /** It's private because I wanted all this data to be prepared 'just in time' when the action happens */
  private case class RunningItem (
    var in:Item = Nil,
    var action:ACTION = null,
    var rangeX:Int = 0,
    var rangeY:Int = 0,
    var damage:Int = 0,
    var use:(RunningItem, ActorRef, Character)=>Option[Item] = (_,_,_)=>None,
    var pickup:(RunningItem, ActorRef, Character)=>Option[Item] = (_,_,_)=>None
  )

  private def prepare(attributes:Item, map:ActorRef, char:Character, item:RunningItem = RunningItem()):RunningItem = {
    attributes.foreach(apply(_, item, map, char))
    item
  }

  def use(item:Item, map:ActorRef, char:Character):Option[Item] = {
    val i = RunningItem(in=item)
    prepare(item, map, char, i).use(i, map, char)
  }

  def pickup(item:Item, map:ActorRef, char:Character):Option[Item] = {
    val i = RunningItem(in=item)
    prepare(item, map, char, i).pickup(i, map, char)
  }

  // Attributes of items
  private def apply(attr:ITEM_ATTR, item:RunningItem, map:ActorRef, char:Character) {attr match {
    case SWORDATTR => item.use=sword_attack
    case HEALATTR  => item.use=heal_attack
    case BASIC_DMG => item.damage+=1
  }}

  // Below are functions that can be attached to use, pickup, etc.
  private def sword_attack(item:RunningItem, map:ActorRef, char:Character):Option[Item] = {
    map tell (DirAction(action= SWORD,rangeX= item.rangeX,rangeY= item.rangeY,direction= char.my.direction, damage= item.damage), char.self)
    Some(item.in)
  }

  private def heal_attack(item:RunningItem, map:ActorRef, char:Character):Option[Item] = {
    map  tell (DirAction(action= HEAL,rangeX= item.rangeX,rangeY= item.rangeY,direction= char.my.direction, damage= item.damage), char.self)
    Some(item.in)
  }
}
