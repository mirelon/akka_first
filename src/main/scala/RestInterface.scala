package akka_first
import akka_first.LunchProtocol._
import spray.httpx.SprayJsonSupport._
import akka.actor._

import spray.routing._
import spray.http.StatusCodes
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}
import akka.pattern.{ask, pipe}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author miso
 */
class RestInterface extends HttpServiceActor with HttpService with ActorLogging {
  actor: Actor =>

  implicit val timeout = Timeout(10 seconds)

  val indexer = context.actorSelection("akka.tcp://akka-first-actor-system@localhost:5002/user/indexer")
  val searcher = context.actorOf(Props[Searcher])

  override def receive: Actor.Receive = runRoute(routes)

  def routes: Route =
    logRequest("routing reached") {
      logResponse("response reached") {
        path("lunches") {
          delete { requestContext =>
            (indexer ? DeleteIndex) andThen {
              case Success(_) => requestContext.complete(StatusCodes.OK)
              case Failure(_) => requestContext.complete(StatusCodes.InternalServerError)
            }
          } ~
          put { requestContext =>
            indexer ! IndexAllLunches
            requestContext.complete(StatusCodes.Accepted)
          } ~
          post {
            logRequest("post reached") {
              entity(as[Lunches]) { lunches => requestContext =>
                log.debug("Got put Lunches request")
                val responder = createResponder(requestContext)
                indexer.ask(lunches).pipeTo(responder)
              }
            }
          } ~
          get {
            logRequest("get reached") { requestContext =>
              log.debug("responder reached")
              val responder = createResponder(requestContext)
              log.debug("responder created")
              searcher.ask(GetLunches).pipeTo(responder)
              log.debug("piped")
            }
          }
        }
      }
    }

  def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext)))
  }
}

class Responder(requestContext:RequestContext) extends Actor with ActorLogging {

  override def receive: Receive = {

    case Lunches(lunches) =>
      log.debug("Responder Lunches")
      requestContext.complete(StatusCodes.OK, lunches)
      self ! PoisonPill
  }
}
