package akka_first

import akka.actor.{Props, ActorLogging, PoisonPill, Actor}
import akka.pattern.ask
import akka.util.Timeout
import com.codemettle.akkasolr.Solr
import com.codemettle.akkasolr.querybuilder.SolrQueryStringBuilder.{RawQuery, QueryPart}
import com.codemettle.akkasolr.solrtypes.SolrQueryResponse
import org.apache.solr.common.SolrException

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class Indexer extends Actor with ActorLogging {
  import LunchProtocol._
  import spray.json._
  import context.dispatcher

  implicit val timeout = Timeout(10 seconds)

  val url = "http://localhost:8983/solr/obedy"

  val jsonFetcher = context.actorOf(Props[JsonFetcher])

  def receive = {

    case IndexAllLunches => {
      log.debug(s"Got request to reindex all lunches")
      jsonFetcher ! IndexAllLunches
    }

    case DeleteIndex => {
      val capturedSender = sender
      val req: Solr.SolrOperation = Solr.Update DeleteByQueryString("*:*")
      (Solr.Client.manager ? Solr.Request(url, req)).andThen {
        case Success(value) => {
          capturedSender ! Success
        }
        case Failure(NonFatal(e)) => {
          log.error(s"Error communicating with SOLR: ${e}")
          capturedSender ! Failure
        }
      }
    }

    case Lunches(lunches) => {
      log.debug(s"Indexer got ${lunches.size} lunches for indexing")
      val req: Solr.SolrOperation = Solr.Update AddDocs (lunches.map(_.toSolr):_*) commit true
      val resp: Future[SolrQueryResponse] =
        (Solr.Client.manager ? Solr.Request(url, req)).mapTo[SolrQueryResponse]
      log.info(s"Sent POST request to Solr to add ${lunches.size} documents")
      resp.onComplete {
        case Success(value) => {
          log.debug(s"Solr returned ${value.status}")
          value.status match {
            case SolrException.ErrorCode.UNKNOWN.code ⇒ log.info(s"Succesfully added ${value.results.documents.size} documents to Solr")
            case _ ⇒ log.error(s"Error adding to SOLR: ${value}")
          }
        }
        case Failure(NonFatal(e)) => log.error(s"Error communicating with SOLR: ${e}")
      }
    }
  }
}
