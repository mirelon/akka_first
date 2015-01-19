import akka.actor.{Props, ActorSystem}

/**
 * @author miso
 */
case object Raven

object First extends App {
  println("Hello world!")
  val system = ActorSystem("akka-first-actor-system")
  val mila_jovovich = system.actorOf(Props(new Actress), "mila_jovovich")
  mila_jovovich ! Raven
}
