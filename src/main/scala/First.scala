package akka_first
import akka.actor.{ActorRef, Props, ActorSystem}
import akka.cluster.Cluster
import spray.can.Http
import akka.io.IO

/**
 * @author miso
 */
case object Raven

object First extends App {
  println("Hello world!")
  implicit val system = ActorSystem("akka-first-actor-system")
  println(s"Starting node with roles: ${Cluster(system).selfRoles}")
  val roles = system.settings.config.getStringList("akka.cluster.roles")
  if(roles.contains("rest")) {
    val milaJovovich = system.actorOf(Props(new Actress), "mila_jovovich")

    val cluster = Cluster(system)
    val joinAddress = cluster.selfAddress
    cluster.join(joinAddress)
    val restInterface: ActorRef = system.actorOf(Props[RestInterface])
    IO(Http) ! Http.Bind(listener = restInterface, interface = "localhost", port = 5000)

    milaJovovich ! Raven
  } else if(roles.contains("indexer")) {
    system.actorOf(Props[Indexer], "indexer")
  } else if(roles.contains("soap")) {
    val soapInterface: ActorRef = system.actorOf(Props[SoapInterface])
    IO(Http) ! Http.Bind(listener = soapInterface, interface = "localhost", port = 5000)
  }





}
