package roshan
import akka.actor.{ActorSystem, ActorRef}
import roshan.protocols.MapProtocol.AreaMessage
import roshan.db.dbCharacter
import roshan.model.Direction

object Useful {
  val tilesPerMap = 20

  var TestActorSystem:Option[ActorSystem] = None
  var isTesting:Boolean = false

  def testing:ActorSystem = {
    isTesting = true
    val sys = ActorSystem("TestingSystem")
    TestActorSystem = Option(sys)
    sys
  }

  val defaultCharacter = dbCharacter(None, 1, 0, 30, Direction.S, Nil, 10, 10)
  private var maxIdForTesting:Int = 1
  def testCharacter:dbCharacter = {maxIdForTesting+=1;defaultCharacter.copy(id=Some(maxIdForTesting))}


  def mapSection(x:Int, y:Int):(Int, Int) =
    ((x / tilesPerMap) * tilesPerMap, (y / tilesPerMap) * tilesPerMap)

  def relativeToMapSection(x:Int, y:Int):(Int, Int) =
    (x - ((x / tilesPerMap) * tilesPerMap), y - ((y / tilesPerMap) * tilesPerMap))

  def getTileRelativeToMapSection(x:Int, y:Int):Int =  {
    val (mapX, mapY) = relativeToMapSection(x, y)
    mapY * tilesPerMap + mapX
  }

  def splitMessage(message:AreaMessage, recipient:ActorRef) {
    val topLeftSection = mapSection(message.x, message.y)
    val bottomRightSection = mapSection(message.x2, message.y2)
    if (topLeftSection != bottomRightSection) {
      val newUpperBoundX = if (message.x2 > topLeftSection._1 + tilesPerMap) topLeftSection._1 + tilesPerMap else message.x2
      val newUpperBoundY = if (message.y2 > topLeftSection._2 + tilesPerMap) topLeftSection._2 + tilesPerMap else message.y2
      recipient ! AreaMessage(message.x, message.y, newUpperBoundX, newUpperBoundY, message.message)
      if (message.x2 > newUpperBoundX)
        splitMessage(AreaMessage(newUpperBoundX, message.y, message.x2, message.y2, message.message), recipient)
      if (message.y2 > newUpperBoundY)
        splitMessage(AreaMessage(message.x, newUpperBoundY, message.x2, message.y2, message.message), recipient)
      if (message.x2 > newUpperBoundX && message.y2 > newUpperBoundY)
        splitMessage(AreaMessage(newUpperBoundX, newUpperBoundY, message.x2, message.y2, message.message), recipient)
    } else recipient ! message
  }
}
