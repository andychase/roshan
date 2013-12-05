package roshan

import akka.actor.{ Actor, Props }
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress

/** Actor that communicates with clients over TCP */
class Network(port:Int) extends Actor  {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", port))

  def receive = {
    case b @ Bound(localAddress) =>
    // do some logging or setup ...

    case CommandFailed(_: Bind) => context stop self

    case c @ Connected(remote, local) =>
      val handler = context.actorOf(Props[Client])
      val connection = sender
      connection ! Register(handler)
  }
}
