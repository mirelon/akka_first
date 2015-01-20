package akka_first
import akka.actor.{ActorRef, Props, ActorSystem}
import spray.can.Http
import akka.io.IO

/**
 * @author miso
 */
case object Raven

object First extends App {
  println("Hello world!")
  implicit val system = ActorSystem("akka-first-actor-system")
  val milaJovovich = system.actorOf(Props(new Actress), "mila_jovovich")
  val restInterface: ActorRef = system.actorOf(Props[RestInterface])
  IO(Http) ! Http.Bind(listener = restInterface, interface = "localhost", port = 5000)

  milaJovovich ! Raven
}
