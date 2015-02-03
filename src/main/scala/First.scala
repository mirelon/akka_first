package akka_first

import javax.xml.ws.Endpoint

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.cluster.Cluster
import cc.notsoclever.customerservice.CustomerServicePortImpl
import org.apache.cxf.transport.http.HttpDestinationFactory
import org.apache.cxf.transport.http_jetty.JettyDestinationFactory
import org.apache.cxf.{BusFactory, Bus}
import spray.can.Http
import akka.io.IO
import timing.Profiler

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
    System.out.println("Starting SOAP Server")
    val bus: Bus = BusFactory.getDefaultBus
    bus.setExtension(new JettyDestinationFactory, classOf[HttpDestinationFactory])
    Endpoint.publish("http://localhost:5000/CustomerServicePort", new CustomerServicePortImpl(system))
    System.out.println("SOAP Server ready...")
  } else if(roles.contains("profiler")) {
    system.actorOf(Props[Profiler], "profiler")
  } else if(roles.contains("meal_processor")) {
    system.actorOf(Props[Profiler], "profiler")
  }




}
