package roshan.model

import akka.actor.ActorRef
import scala.collection.mutable

class Grid(val sizeX:Int, val sizeY:Int, val offsetX:Int, val offsetY:Int) {
  var characterMap = Array.fill[List[ActorRef]](sizeX*sizeY)(Nil)

  var characterPosition = new mutable.HashMap[ActorRef, (Int, Int)]()
  var collidable = new mutable.HashSet[ActorRef]()
  
  def coord(x:Int, y:Int):Int  = (x-offsetX)+(y-offsetY)*sizeX
  def coord(xy:(Int, Int)):Int = coord(xy._1, xy._2)

  def add(character: ActorRef, x: Int, y: Int, isCollidable:Boolean = true) {
    characterMap(coord(x,y)) = character :: characterMap(coord(x,y))
    characterPosition += character -> (x, y)
    if (isCollidable)
      collidable += character
  }

  def remove(character:ActorRef) {
    val xy = coord(characterPosition(character))
    characterMap(xy) = characterMap(xy).filter(_ != character)
    characterPosition -= character
    collidable -= character
  }

  def move(character: ActorRef, x:Int, y:Int) {
    remove(character)
    add(character, x, y)
  }

  def getAt(xy:(Int, Int)):List[ActorRef] =
    characterMap(coord(xy))

  def forAllCharacters(function:(ActorRef)=>Unit) {
    characterMap foreach (_ foreach function)
  }

  def checkCharacterCollision(xy:(Int, Int)):Boolean = {
    for(char <- characterMap(coord(xy)) if collidable contains char)
        return true
    false
  }
}
