package roshan

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}
import java.net.InetSocketAddress

/** Actor that communicates with clients over TCP */
class Network() extends Actor {
  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8081))

  def receive = {
    case b@Bound(localAddress) =>
    // do some logging or setup ...

    case CommandFailed(_: Bind) => context stop self

    case c@Connected(remote, local) =>
      val handler = context.actorOf(Props(classOf[Client], sender()))
      val connection = sender()
      connection ! Register(handler)
  }
}
