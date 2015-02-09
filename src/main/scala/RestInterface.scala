package akka_first
import akka_first.LunchProtocol._
import slick.{Request, Requests}
import spray.httpx.SprayJsonSupport._
import akka.actor._

import spray.routing._
import spray.http.StatusCodes
import akka.util.Timeout
import timing.ProfilingProtocol.Start
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}
import akka.pattern.{ask, pipe}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.driver.FreeSQLServerDriver.simple._
import Database.dynamicSession

/**
 * @author miso
 */
class RestInterface extends HttpServiceActor with HttpService with ActorLogging {
  actor: Actor =>

  implicit val timeout = Timeout(100 seconds)

  val indexer = context.actorSelection("akka.tcp://akka-first-actor-system@localhost:5002/user/indexer")
  val profiler = context.actorSelection("akka.tcp://akka-first-actor-system@localhost:5003/user/profiler")
  val searcher = context.actorOf(Props[Searcher])
  val jdbcUrl = "jdbc:jtds:sqlserver://10.64.172.21:1433/VyhladavanieTest"
  val db = Database.forURL(jdbcUrl, driver = "net.sourceforge.jtds.jdbc.Driver", user="VyhladavanieTest", password=sys.env("MSSR_DB_PASS"))
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
            profiler ! Start("Whole indexing")
            db.withDynSession {
              val requests = TableQuery[Requests]
              requests += Request(None, "name47")
              log.info(requests.drop(3).take(3).run.toString)
            }
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
