package roshanTest

import akka.actor._
import akka.testkit.{TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterEach, WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import roshan._

class ServerTest(_system: ActorSystem) extends TestKit(_system)
    with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  def this() = this(Useful.testing)

  "Server" should {
    "Choose Appropriate Map Box" in {
      Server.mapBoxNumber(0, 0) should be === 0
      Server.mapBoxNumber(21, 0) should be === 1
      Server.mapBoxNumber(0, 21) should be === Server.mapBoxesX
      Server.mapBoxNumber(21, 21) should be === Server.mapBoxesX+1
      if (Server.mapBoxesX*Server.mapBoxesY > 4)
        Server.mapBoxNumber(41, 21) should be === Server.mapBoxesX+2

      Server.mapBoxXFromI(Server.mapBoxesX) should be === 0
      Server.mapBoxYFromI(Server.mapBoxesX) should be === 20

      Server.mapBoxXFromI(Server.mapBoxesX+1) should be === 20
      Server.mapBoxYFromI(Server.mapBoxesX+1) should be === 20

    }

    "Have correctly ordered Map Boxes" in {
      val name = Server.mapBox(21, 0).toString()
      name contains "map20,0"
    }
  }
}