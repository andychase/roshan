package roshan.db

import roshan.model.Direction.Direction
import roshan.protocols.CharacterProtocol._

import scala.slick.driver.SQLiteDriver.simple._
import TupleMethods._

class MapTable(tag: Tag) extends Table[(Int, Int, Int, String)](tag, "maps") {
  def x = column[Int]("x", O.PrimaryKey)

  def y = column[Int]("y", O.PrimaryKey)

  def layer = column[Int]("layer")

  def data = column[String]("data")

  def * = x ~ y ~ layer ~ data
}

case class dbCharacter(
                        var id: Option[Int],
                        var speed: Int,
                        var ticks: Int,
                        var health: Int,
                        var direction: Direction,
                        var items: Inventory,
                        var x: Int,
                        var y: Int
                        )


class VersionTable(tag: Tag) extends Table[Int](tag, "versions") {
  def version = column[Int]("version", O.PrimaryKey)

  def * = version
}