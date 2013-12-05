package roshanTest

import akka.actor._
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterEach, WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import roshan.buffer.Msg.ACTION._
import roshan.buffer.Msg.{MapLayer, MapData, ACTION}
import roshan.model.Direction
import scala.concurrent.duration._
import roshan.map.MapBox
import roshan.Character
import scala.collection.JavaConversions.asJavaIterable
import roshan.protocols.CharacterChangesProtocol.CharacterChangeBroadcast
import roshan.protocols.LoaderProtocol.ReceiveMap
import scala.Some
import roshan.protocols.MapProtocol.Walk
import roshan.protocols.CharacterChangesProtocol.Subscribe
import roshan.protocols.MapProtocol.DirAction
import roshan.protocols.MapProtocol.AddCharacter
import roshan.db.dbCharacter
import roshan.protocols.MapProtocol.SendAllUsers
import roshan.protocols.MapProtocol.Say
import roshan.{Loaderable, Mappable, Useful}

class MapTest(_system: ActorSystem) extends TestKit(_system)
    with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {
  var map:TestActorRef[MapBox] = null

  def this() = this(Useful.testing)

  override def afterAll() {
    system.shutdown()
  }

  override def beforeEach() {
    map = TestActorRef(new MapBox(Server = ServerMock))
  }

  val log = system.log

  object ServerMock extends Mappable with Loaderable {
    def sendMap(x: Int, y: Int, recipient: ActorRef) {}
    def saveChar(char: dbCharacter, sender: ActorRef) {}
    def mapBox(x: Int, y: Int):ActorRef = map
  }

  "Map" should {
    "accept new characters" in {
      TestActorRef(new Character(null, Server = ServerMock, character = Useful.testCharacter))
      map ! SendAllUsers(0, 0)
      val response = expectMsgClass(classOf[CharacterChangeBroadcast])
      response.change.getX should be === Useful.testCharacter.x
    }

    "handle character attacks and collisions" in {
      map.underlyingActor.subscribe(self)

      val other = TestActorRef(new Character(null, Server = ServerMock, character = Useful.testCharacter))
      val other_id = other.underlyingActor.my.id.get
      val attacker = TestActorRef(new Character(null, Server = ServerMock, character = Useful.testCharacter))
      val attacker_id = attacker.underlyingActor.my.id.get

      attacker_id should not equal other_id

      // New Character Messages
      expectMsgAllClassOf(classOf[CharacterChangeBroadcast], classOf[CharacterChangeBroadcast])

      // Scoot actor down for attacking (ignoring collisions since no maps are loaded)
      map.underlyingActor.grid.move(other, 10, 10)
      map.underlyingActor.grid.move(attacker, 10, 11)

      // Attack
      map tell (DirAction(ACTION.SWORD, 1, 0, Direction.N, 5), attacker)

      var (sword, hurt) = (false, false)
      val messages = expectMsgAllClassOf(classOf[CharacterChangeBroadcast], classOf[CharacterChangeBroadcast])
      for (m <- messages) m.change.hasAction should be === true
      for (m <- messages) m.change.getAction match {
        case SWORD => sword = true; expectResult(attacker_id.underlying())(m.change.getId)
        case HURT  => hurt = true; expectResult(other_id.underlying())(m.change.getId)
        case _     => assert(condition = false, "Unknown action %s".format(m.toString))
      }
      (sword, hurt) should be === (true, true)
      expectNoMsg(10 milliseconds)
    }

    "handle things spoken" in {
      map ! AddCharacter(0, 15, 15)
      map ! Subscribe()
      map ! Say("HEY")
      val broadcast = expectMsgClass(classOf[CharacterChangeBroadcast])
      broadcast.change.getSay should be === "HEY"
    }

    "handle characters moving" in {
      map ! AddCharacter(1, 15, 15)
      map ! Subscribe()
      val ly = MapLayer.newBuilder().addAllTile _
      def buildBlankMap = MapData.newBuilder().addAllLayer(
          0 to 4 map {_=>ly(0 to 20*20 map {_=>new Integer(0)}).build}
        ).build()
      map ! ReceiveMap(0,0, buildBlankMap)
      map ! Walk(Direction.S, 1)
      val broadcast = expectMsgClass(classOf[CharacterChangeBroadcast])
      (broadcast.change.getX, broadcast.change.getY) should be === (15, 16)
      // Handle Collisions
      map.underlyingActor.grid.move(self, 10, 11)
      TestActorRef(new Character(null, Server = ServerMock, character = Useful.testCharacter.copy(id=Some(2),x=10,y=10)))
      expectMsgClass(classOf[CharacterChangeBroadcast])
      map ! Walk(Direction.N, 1)
      expectNoMsg(10 milliseconds)
    }

    "handle forallcharacters" in {
      def add(x:Int,y:Int):ActorRef = {
        TestActorRef(new Character(null, Server = ServerMock, character = Useful.testCharacter.copy(x=x,y=y)))
      }
      val chars = add(0,0) ::
        add(19,0) ::
        add(0,19) ::
        add(19,19) :: Nil
      var count = 0
      map.underlyingActor.grid.forAllCharacters({c=>count += 1})
      count should be === chars.size
    }
}
}
