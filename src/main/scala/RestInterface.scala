package akka_first
import akka_first.LunchProtocol._
import spray.httpx.SprayJsonSupport._
import akka.actor._

import spray.routing._
import spray.http.StatusCodes
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * @author miso
 */
class RestInterface extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging {
  actor: Actor =>

  import context.dispatcher

  implicit val timeout = Timeout(10 seconds)

  import akka.pattern.ask
  import akka.pattern.pipe

  val indexer = context.actorOf(Props[Indexer])
  val searcher = context.actorOf(Props[Searcher])

  def routes: Route =
    logRequest("routing reached") {
      logResponse("response reached") {
        path("lunches") {
          put { requestContext =>
            indexer ! IndexAllLunches
            requestContext.complete(StatusCodes.Accepted)
          } ~
          post {
            logRequest("post reached") {
              entity(as[Lunches]) { lunches => requestContext =>
                log.debug("Got put Lunches request")
                val responder = createResponder(requestContext)
                println(s"Sending lunches to indexer: ${lunches}")
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
    context.actorOf(Props(new Responder(requestContext, indexer)))
  }

}

class Responder(requestContext:RequestContext, indexer:ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {

    case Lunches(lunches) =>
      log.debug("Responder Lunches")
      requestContext.complete(StatusCodes.OK, lunches)
      self ! PoisonPill
  }
}
