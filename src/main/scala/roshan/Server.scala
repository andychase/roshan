package roshan

import akka.actor.{ActorRef, Props, ActorSystem}
import roshan.db.{dbCharacter, Loader}
import roshan.Useful._
import roshan.protocols.LoaderProtocol.{SaveCharacter, SendMap, LoadCharacter}
import roshan.map.MapBox

trait Mappable {
  def mapBox(x:Int, y:Int):ActorRef
  def mapBox(xy:(Int, Int)):ActorRef = mapBox(xy._1, xy._2)
}

trait Loaderable {
  def sendMap(x:Int, y:Int, recipient:ActorRef)
  def saveChar(char:dbCharacter, sender:ActorRef)
}

/** This is the top level of the server.
  * Holds the game pieces and links them together
  * EVERYTHING SHOULD STAY IMMUTABLE IN HERE.
  * Lazy vals because App doesn't work well with testing otherwise. */
object Server extends App with Mappable with Loaderable {
  // Setup --------------------
  lazy val system = TestActorSystem getOrElse ActorSystem("LittleIslandSystem")

  // Setup primary servers
  val network = if (isTesting) null else system.actorOf(Props[Network](new Network(8081)), "network")
  lazy val loader = system.actorOf(Props[Loader], "loader")
  lazy val login = system.actorOf(Props[Login], "login")

  // Bring up the map servers
  lazy val (mapBoxesX, mapBoxesY) = (2, 2)
  // Initialize the MapBoxes
  lazy val MapBoxes:Array[ActorRef] = (
    for (y <- 0 until mapBoxesY*tilesPerMap by tilesPerMap;
         x <- 0 until mapBoxesX*tilesPerMap by tilesPerMap)
      yield system.actorOf(Props[MapBox](new MapBox(x, y)), "map%d,%d".format(x, y))
  ).toArray[ActorRef]

  // Send maps to map boxes
  for ((box, i) <- MapBoxes.zipWithIndex)
    loader ! SendMap(mapBoxXFromI(i), mapBoxYFromI(i), box)

  // Functions -----------------
  def sendMap(x:Int, y:Int, recipient:ActorRef)   { loader tell (SendMap(x, y, recipient), recipient) }
  def saveChar(char:dbCharacter, sender:ActorRef) { loader tell (SaveCharacter(char), sender) }

  def register(client:ActorRef) {
    loader tell (LoadCharacter(Useful.defaultCharacter), client)
  }

  // Map Box functions
  def mapBox(x:Int, y:Int):ActorRef  = MapBoxes(mapBoxNumber(x, y))
  def mapBoxNumber(x:Int, y:Int):Int =
    (Useful.mapSection(x, y)._1 / tilesPerMap)+((Useful.mapSection(x, y)._2 / tilesPerMap)*mapBoxesX)
  def mapBoxXFromI(i:Int) = (i % mapBoxesX) * tilesPerMap
  def mapBoxYFromI(i:Int) = (i / mapBoxesX) * tilesPerMap
}