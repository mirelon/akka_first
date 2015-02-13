package akka_first
import akka.actor.{Props, Actor, ActorLogging}
import akka.util.Timeout
import akka_first.LunchProtocol.{Lunch, Lunches, IndexAllLunches}
import com.codemettle.akkasolr.Solr
import com.codemettle.akkasolr.solrtypes.SolrQueryResponse
import org.apache.solr.common.SolrException
import spray.client.pipelining._
import spray.http.HttpRequest
import akka.pattern.ask
import timing.ProfilingProtocol.{Stop, Start}

import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Random, Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * @author miso
 */
class JsonFetcher extends Actor with ActorLogging {
  implicit val timeout = Timeout(100 seconds)
  val profiler = context.actorSelection("akka.tcp://akka-first-actor-system@localhost:2550/user/profiler")

  def receive = {
    case IndexAllLunches => {
      val testTwoActors = false
      val testProcessorActor = true
      val capturedSender = sender
      val url = "https://spreadsheets.google.com/feeds/list/0AmHPGqfSTzZhdE9qRzdIdU5qdS1RZkZINGx4aUl6a3c/od6/public/values?alt=json"
      val pipeline: HttpRequest => Future[Lunches] = sendReceive ~> unmarshal[Lunches]
      profiler ! Start("Spreadsheet API")
      val response = pipeline(Get(url))
      log.info(s"Sent request to ${url}")
      response.onComplete {
        case Success(value) => {
          log.debug("Got spreadsheet response")
          profiler ! Stop("Spreadsheet API")
          profiler ! Start("Preparing big data")

          val big_data_2 = Lunches((0 to 9999).map (i => {
            val r = new Random(i)
            Lunch(meal = r.nextString(200), restaurant = r.nextString(100), date = value.lunches(i % value.lunches.size).date)
          }).toVector)
          profiler ! Stop("Preparing big data")
          profiler ! Start("Processing big data")
          val big_data = if(testProcessorActor) {
            new Lunches(Await.result(Future.sequence(big_data_2.lunches.map(x => {
              val restaurantProcessor = context.actorOf(Props[RestaurantProcessor])
              (restaurantProcessor ? x).mapTo[Lunch]
            })), 1000 seconds))
          } else {
            new Lunches(big_data_2.lunches.map(x => new Lunch(meal = x.meal, restaurant = "prefix_" + x.restaurant, date = x.date)).toVector)
          }
          profiler ! Stop("Processing big data")
          profiler ! Start("Processing response")
          if (testTwoActors) {
            capturedSender ! big_data
          } else {
            val solrUrl = "http://localhost:8983/solr/obedy"
            val lunches = big_data.lunches
            profiler ! Start("Adding to SOLR")
            val req: Solr.SolrOperation = Solr.Update AddDocs (lunches.map(_.toSolr):_*) commit true
            val resp: Future[SolrQueryResponse] =
              (Solr.Client.manager ? Solr.Request(solrUrl, req)).mapTo[SolrQueryResponse]
            log.info(s"Sent POST request to Solr to add ${lunches.size} documents")
            resp.onComplete {
              case Success(value) => {
                log.debug(s"Solr returned ${value.status}")
                value.status match {
                  case SolrException.ErrorCode.UNKNOWN.code ⇒ {
                    log.info(s"Succesfully added ${lunches.size} documents to Solr")
                    profiler ! Stop("Adding to SOLR")
                    profiler ! Stop("Processing response")
                    profiler ! Stop("Whole indexing")
                  }
                  case _ ⇒ log.error(s"Error adding to SOLR: ${value}")
                }
              }
              case Failure(NonFatal(e)) => log.error(s"Error communicating with SOLR: ${e}")
            }
          }
        }
        case Failure(NonFatal(e)) => {
          log.error(s"Failure when communicating with spreadsheet API: ${e}")
        }
      }

    }
  }
}
