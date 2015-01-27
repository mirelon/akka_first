package akka_first

import akka.actor.{Props, PoisonPill, Actor, ActorLogging}
import akka.util.Timeout
import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes
import spray.routing._
import akka.util.Timeout
import scala.concurrent.duration._
import akka_first.LunchProtocol._
import akka.pattern.{ask, pipe}

import scalaxb.`package`._

/**
 * @author miso
 */
class SoapInterface extends HttpServiceActor with HttpService with ActorLogging {
  actor: Actor =>

  implicit val timeout = Timeout(10 seconds)

  override def receive: Actor.Receive = runRoute(routes)

  def routes: Route =
    logRequest("routing reached") {
      path("obedy") { requestContext =>
        log.info("obedy reached")
        val responder = createResponder(requestContext)
        responder ! Obedy(Obed(date="dnes", meal="zavitky", restaurant = "vegan"))
      }
    }

  def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new SoapResponder(requestContext)))
  }
}

class SoapResponder(requestContext:RequestContext) extends Actor with ActorLogging with XMLProtocol {

  override def receive: Receive = {
    case obedy: Obedy =>
      log.debug("Responder Lunches")
      requestContext.complete(StatusCodes.OK, toXML[Obedy](obedy, "obedy", defaultScope))

      self ! PoisonPill
  }
}
