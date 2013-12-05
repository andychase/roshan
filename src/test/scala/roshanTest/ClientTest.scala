package roshanTest

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterEach, WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import roshan.{Useful, Map, Client}

class ClientTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  def this() = this(Useful.testing)
  var clientRef:TestActorRef[Client] = _
  var client:Client = _
  var map:TestActorRef[Map] = _
  override def afterAll() {
    system.shutdown()
  }

  override def beforeEach() {
    //clientRef = TestActorRef(new Client(null))
    //map = TestActorRef(new Map())
    //client = clientRef.underlyingActor
  }

  "Client" should {
    "Create a character" in {
      //expectMsgClass(classOf[LoadCharacter])
    }
  }
}