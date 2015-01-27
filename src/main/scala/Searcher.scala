package akka_first
import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import akka.pattern.ask
import com.codemettle.akkasolr.Solr
import com.codemettle.akkasolr.solrtypes.SolrQueryResponse
import org.apache.solr.common.SolrException

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
 * @author miso
 */
class Searcher extends Actor with ActorLogging {
  import LunchProtocol._
  import spray.json._

  implicit val timeout = Timeout(10 seconds)

  def receive = {
    case GetLunches => {
      val capturedSender = sender
      val req: Solr.SolrOperation = Solr.Select(Solr.createQuery("*:*").toParams)
      val resp: Future[SolrQueryResponse] =
        (Solr.Client.manager ? Solr.Request("http://localhost:8983/solr/obedy", req)).mapTo[SolrQueryResponse]
      resp.onComplete {
        case Success(value) => {
          log.debug(s"Solr returned ${value.status}")
          log.debug(s"Results: ${value.results.documents.map(_.asMap).map(doc => Lunch.fromMap(doc))}")
          value.status match {
            case SolrException.ErrorCode.UNKNOWN.code ⇒ {
              log.info(s"Succesfully selected from SOLR: ${value}")
              val lunches = Lunches(value.results.documents.map(_.asMap).map(doc => Lunch.fromMap(doc)).to[Vector])
              capturedSender ! lunches
            }
            case _ ⇒ log.error(s"Error selecting from SOLR: ${value}")
          }
        }
        case Failure(NonFatal(e)) => log.error(s"Error communicating with SOLR: ${e}")
      }
    }
  }
}
