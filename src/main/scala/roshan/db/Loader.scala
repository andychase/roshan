package roshan.db

import akka.actor.{Actor, ActorLogging}
import roshan.Useful
import roshan.buffer.Msg.{MapData, MapLayer}
import roshan.protocols.LoaderProtocol.{ReceiveMap, SendMap, SendMaps}

import scala.slick.driver.SQLiteDriver.simple._


class Loader() extends Actor with ActorLogging {
  val getMaps = for {
    map <- TableQuery[MapTable] if map.layer is 0
  } yield (map.x, map.y)

  val getLayers = for {
    (x, y) <- Parameters[(Int, Int)]
    map <- TableQuery[MapTable] if (map.x is x) && (map.y is y)
  } yield map.data

  def receive = {
    case SendMaps() =>
      Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
        implicit session =>
          getMaps foreach {
            case (x, y) =>
              self ! SendMap(x, y, sender)
          }
      }

    case SendMap(x, y, recipient) =>
      val map_buffer = MapData.newBuilder()
      map_buffer setXSize Useful.tilesPerMap
      map_buffer setYSize Useful.tilesPerMap
      map_buffer setXOffset x
      map_buffer setYOffset y
      Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
        implicit session =>
          getLayers(x, y) foreach {
            case (data) =>
              val layer = MapLayer.newBuilder()
              data.split(",").map(layer addTile _.toInt)
              map_buffer addLayer layer
          }
          recipient ! ReceiveMap(x, y, map_buffer.build)
      }

    case _ => log info "Loader received unknown message"
  }
}

