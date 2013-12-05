import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterEach, WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import roshan.buffer.Msg.ACTION._
import roshan.buffer.Msg.ITEM_ATTR._
import roshan.model.Direction
import roshan.protocols.CharacterProtocol.Action
import roshan.protocols.MapProtocol.DirAction
import roshan.{Useful, Character}

class ItemTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  def this() = this(Useful.testing)
  var characterRef:TestActorRef[Character] = _
  var character:Character = _
  override def afterAll() {
    system.shutdown()
  }

  override def beforeEach() {
    characterRef = TestActorRef(new Character(self))
    character = characterRef.underlyingActor
    character.map = self
  }

  "Item Factory" should {
    "Activate Items" in {
    }
  }

  "Character" should {
    "Use Items" in {
      // Sword Attr provides sword action
      character.my.items = List(List(SWORDATTR))
      characterRef ! Action(SWORD, Direction.S, null)
      val attack = expectMsgClass(classOf[DirAction])
      attack.action should be === SWORD
      attack.damage should be === 0
      // No List to make sure it at least works
      character.my.items = List(List())
      characterRef ! Action(SWORD, Direction.S, null)
      // Heal
      character.my.items = List(List(HEALATTR))
      characterRef ! Action(SWORD, Direction.S, null)
      val heal = expectMsgClass(classOf[DirAction])
      heal.action should be === HEAL
      // Basic
      character.my.items = List(List(SWORDATTR, BASIC_DMG))
      characterRef ! Action(SWORD, Direction.S, null)
      val attack2 = expectMsgClass(classOf[DirAction])
      attack2.action should be === SWORD
      attack2.damage should be === 1
    }
  }
}