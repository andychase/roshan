package roshan

import akka.actor._
import collection.mutable
import java.net.InetSocketAddress
import roshan.protocols.ClientProtocol._

/** Actor that communicates with clients over TCP */
class Network(port:Int) extends Actor with ActorLogging {
  var clients = new mutable.HashMap[IO.Handle, ActorRef]()

  override def preStart() { IOManager(context.system) listen new InetSocketAddress(port) }

  def receive = {
    case IO.NewClient(server) =>
      val clientHandle = server.accept()
      val client = Server.clientConnected(clientHandle)
      clients += clientHandle -> client

    case IO.Read(clientHandle, bytes) =>
      clients(clientHandle) ! ReceiveMessage(bytes)

    case IO.Closed(clientHandle, cause) =>
      clients(clientHandle) ! Cleanup()
      clients(clientHandle) ! PoisonPill
      clients.remove(clientHandle)
  }
}
