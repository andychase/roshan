package roshanTest

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterEach, WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import roshan.buffer.Msg.ACTION
import roshan.model.Direction
import roshan.protocols.CharacterProtocol.{Action, Hit}
import roshan.{Useful, Character}
import roshan.protocols.MapProtocol.DirAction


class CharTest(_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  def this() = this(Useful.testing)
  var characterRef:TestActorRef[Character] = null
  var character:Character = null

  override def afterAll() {
    system.shutdown()
  }

  override def beforeEach() {
    characterRef = TestActorRef(new Character(self))
    character = characterRef.underlyingActor
    character.map = self
  }

  "Character" should {
    "Take damage" in {
      val before = character.my.health
      characterRef ! Hit(1, ACTION.SWORD)
      expectMsgClass(classOf[Action])
      character.my.health should be === before - 1
    }

    "Change direction during an action" in {
      characterRef ! Action(ACTION.SWORD, Direction.N, null)
      expectMsgClass(classOf[DirAction])
      character.my.direction should be === Direction.N
    }
  }


}