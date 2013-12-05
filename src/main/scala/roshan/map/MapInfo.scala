package roshan.map

import roshan.model.Grid
import roshan.Useful._
import roshan.{Loaderable, Useful}
import roshan.buffer.Msg.{MapData, LAYERS}
import akka.actor.{ActorRef, Actor}
import roshan.protocols.LoaderProtocol.{ReceiveMap, SendMap}
import roshan.protocols.MapProtocol.Walk

trait MapInfo extends Actor {
  val mapX:Int
  val mapY:Int
  val Server:Loaderable
  var waitingWalkMsg: List[(Walk, ActorRef)]

  var grid = new Grid(tilesPerMap, tilesPerMap, mapX, mapY)
  var tile_map:Option[MapData] = None

  /** If we crash, resend map */
  override def postRestart(cause: Throwable) {
    Server.sendMap(mapX, mapY, self)
  }

  def checkMapCollision(x:Int, y:Int):Boolean = {
    val tileXY = Useful.getTileRelativeToMapSection(x, y)
    (tile_map map ((_:MapData) getLayer LAYERS.COLLISION_VALUE getTile tileXY) getOrElse 0) != 0
  }

  def moveOnOurMap(x:Int, y:Int):Boolean =
    mapSection(x, y) == mapSection(mapX, mapY)

  def HandleMapInfo:Receive = {
    case SendMap(x, y, recipient) =>
      if (tile_map.isDefined) recipient ! ReceiveMap(x, y, tile_map.get)
      else Server.sendMap(x, y, recipient)

    case ReceiveMap(x, y, mapData) =>
      tile_map = Some(mapData)
      waitingWalkMsg.foreach({i:(Walk, ActorRef) => self.tell(i._1, i._2)})
  }
}
