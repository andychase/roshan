package roshan.db

import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.driver.SQLiteDriver.simple._
import scala.collection.immutable.HashMap
import scala.slick.session.Session
import java.sql.SQLException

object Migrate {
  val migrations = HashMap[Int, String]()
  val tables =  VersionTable :: CharacterTable :: MapTable :: Nil

  def migrate()(implicit session:Session) {
    // Create tables if needed
    for(table <- tables)
      try table.ddl.create
      catch { case e:SQLException => }

    // Collect version information
    val dbVersions  = Query(VersionTable).list
    val allVersions = migrations.map(_._1).toList

    // Run Migrations
    for (version:Int <- allVersions diff dbVersions) {
      Q.updateNA(migrations(version)).execute()
      VersionTable insert version
    }
  }
}
