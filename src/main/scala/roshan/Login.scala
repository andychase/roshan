package roshan

import akka.actor.Actor
import roshan.protocols.LoginProtocol.Register

/** The Login Server currently just registers a new character each invocation */
class Login() extends Actor {
  def receive = {
    case Register() =>
      Server.register(sender)
  }
}
