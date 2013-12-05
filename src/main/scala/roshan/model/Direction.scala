package roshan.model

object Direction extends Enumeration {
  type Direction = Value
  val S, SW, W, NW, N, NE, E, SE = Value
  def to_XY(direction:Direction):(Int, Int) = direction match {
    case S  => (0, 1)
    case SW => (-1, 1)
    case W  => (-1, 0)
    case NW => (-1, -1)
    case N  => (0, -1)
    case NE => (1, -1)
    case E  => (1, 0)
    case SE => (1, 1)
  }

  def rotate(direction:Direction, x:Int, y:Int, x2:Int, y2:Int):(Int, Int) = direction match {
    // X Y based on facing S
    case S | SE | SW => (x+x2, y+y2) // 0 0 0 1 => 0 1
    case W           => (x-y2, y+x2) // 0 0 0 1 => 1 0
    case N | NE | NW => (x+x2, y-y2) // 0 0 0 1 => 0 -1
    case E           => (x+y2, y+x2) // 0 0 0 1 => 0 1
  }
}