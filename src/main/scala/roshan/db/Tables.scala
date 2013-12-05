package roshan.db

import scala.slick.driver.SQLiteDriver.simple._
import roshan.model.Direction
import roshan.model.Direction.Direction
import roshan.protocols.CharacterProtocol._
import roshan.buffer.Msg.ITEM_ATTR

object MapTable extends Table[(Int, Int, Int, String)]("maps") {
  def x = column[Int]("x", O.PrimaryKey)
  def y = column[Int]("y", O.PrimaryKey)
  def layer = column[Int]("layer")
  def data = column[String]("data")

  def * = x ~ y ~ layer ~ data
}

case class dbCharacter(
   var id:Option[Int],
   var speed:Int,
   var ticks:Int,
   var health:Int,
   var direction:Direction,
   var items:Inventory,
   var x:Int,
   var y:Int
)

object CharacterTable extends Table[dbCharacter]("characters") {
  implicit object DirectionMapper
    extends scala.slick.lifted.MappedTypeMapper[Direction.Direction, Int]
    with scala.slick.lifted.BaseTypeMapper[Direction.Direction] {
    def map(j: Direction.Direction) = j.id
    def comap(s: Int):Direction.Direction = Direction(s)
  }

  implicit def ITEM_ATTR2Int(i:ITEM_ATTR) = i.getNumber

  implicit object ItemMapper
    extends scala.slick.lifted.MappedTypeMapper[Inventory, String]
    with scala.slick.lifted.BaseTypeMapper[Inventory] {
    def map(i: Inventory) = i.map(_.map(_.getNumber).mkString(",")).mkString("|")
    def comap(s: String):Inventory = s.split("|").map(_.split(",").map({i=>ITEM_ATTR.valueOf(i.toInt)}).toList).toList
  }

  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def speed = column[Int]("speed")
  def ticks = column[Int]("ticks")
  def health = column[Int]("health")
  def direction = column[Direction]("direction")
  def items = column[Inventory]("items")
  def x = column[Int]("x")
  def y = column[Int]("y")

  implicit def int2direction(value:Direction):Int = value.id
  implicit def direction2int(value:Int):Int = Direction(value)

  def * = id ~ speed ~ ticks ~ health ~ direction ~ items ~ x ~ y  <> (dbCharacter, dbCharacter.unapply _)
}

object VersionTable extends Table[Int]("versions") {
  def version = column[Int]("version", O.PrimaryKey)
  def * = version
}