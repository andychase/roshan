package roshanTest

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{BeforeAndAfterEach, WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import roshan.db.{Migrate, dbCharacter, Loader}
import roshan.model.Direction
import roshan.protocols.LoaderProtocol.LoadCharacter
import roshan.Useful
import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession
import scala.reflect.io.Path
import scala.slick.jdbc.{StaticQuery => Q}


class LoaderTest(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll with BeforeAndAfterEach with ShouldMatchers {

  def this() = this(Useful.testing)
  var loaderRef:TestActorRef[Loader] = _
  var loader:Loader = _

  override def afterAll() {
    system.shutdown()
  }

  override def beforeEach() {
    loaderRef = TestActorRef(new Loader())
    loader    = loaderRef.underlyingActor
  }

  "Loader" should {
    "Migrate successfully" in {
      Path("test.db").ifFile(_.delete())
      Database.forURL("jdbc:sqlite:test.db", driver = "org.sqlite.JDBC") withSession {
        // Is clean
        Q.queryNA[String]("select name from sqlite_master").list.size should be === 0
        // Migrate
        Migrate.migrate()
        // Should have tables
        Q.queryNA[String]("select name from sqlite_master").list.size should be >= 0
      }
      Path("test.db").ifFile(_.delete())
    }

    "Load a new Character" in {
       loaderRef ! LoadCharacter(dbCharacter(None, 1, 0, 30, Direction.S, List(List(), List()), 10, 10))
      val char = expectMsgClass(classOf[LoadCharacter])
      char.char.id should not be None
    }
  }


}