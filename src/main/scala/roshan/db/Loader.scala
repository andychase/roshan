package roshan.db

import akka.actor.{ActorLogging, Actor}
import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession
import roshan.protocols.LoaderProtocol._
import roshan.buffer.Msg.{MapLayer, MapData}
import roshan.Useful
import roshan.protocols.LoaderProtocol.SaveCharacter
import roshan.protocols.LoaderProtocol.SendMap
import roshan.protocols.LoaderProtocol.ReceiveMap
import roshan.protocols.LoaderProtocol.SendMaps


class Loader() extends Actor with ActorLogging {
  Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
    Migrate.migrate()
  }

  val getMaps = for {
    map <- MapTable if map.layer is 0
  } yield (map.x, map.y)

  val getLayers = for {
    (x, y) <- Parameters[(Int, Int)]
    map <- MapTable if (map.x is x) && (map.y is y)
  } yield map.data

  def receive = {
    case SendMaps() =>
      Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
        getMaps() foreach {
          case(x, y) =>
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
        getLayers(x, y) foreach {
          case (data) =>
            val layer = MapLayer.newBuilder()
            data.split(",").toArray.map (layer addTile _.toInt)
            map_buffer addLayer layer
        }
        recipient ! ReceiveMap(x, y, map_buffer.build)
      }

    case LoadCharacter(character) if character.id.isEmpty =>
      Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
        val charId = CharacterTable returning CharacterTable.id insert character
        sender ! LoadCharacter(character.copy(id = charId))
      }

    case LoadCharacter(character) =>
        Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
          for (char <- Query(CharacterTable).filter(_.id == character.id.get.underlying()))
            sender ! LoadCharacter(char)
        }

    case SaveCharacter(char) if char.id.isDefined =>
      Database.forURL("jdbc:sqlite:save.db", driver = "org.sqlite.JDBC") withSession {
        // Select character based on id and replace.
        (for { c <- CharacterTable if c.id === char.id } yield c).update(char)
      }

    case _ => log info "Loader received unknown message"
  }
}

