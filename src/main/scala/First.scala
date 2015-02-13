package akka_first

import javax.xml.ws.Endpoint

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.cluster.Cluster
import cc.notsoclever.customerservice.CustomerServicePortImpl
import com.typesafe.config.ConfigFactory
import org.apache.cxf.transport.http.HttpDestinationFactory
import org.apache.cxf.transport.http_jetty.JettyDestinationFactory
import org.apache.cxf.{BusFactory, Bus}
import spray.can.Http
import akka.io.IO
import timing.Profiler

/**
 * @author miso
 */

object First extends App {
  if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
  implicit val system = ActorSystem("ClusterSystem")
  Cluster(system)
  system.actorOf(Props[Indexer], name = "indexer")
  val restInterface: ActorRef = system.actorOf(Props[RestInterface], name = "restInterface")
  IO(Http) ! Http.Bind(listener = restInterface, interface = "localhost", port = args(0).toInt + 2450)
}
