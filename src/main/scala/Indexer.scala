package akka_first

import akka.actor.{Props, ActorLogging, PoisonPill, Actor}
import akka.pattern.ask
import akka.util.Timeout
import com.codemettle.akkasolr.Solr
import com.codemettle.akkasolr.querybuilder.SolrQueryStringBuilder.QueryPart
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

  val jsonFetcher = context.actorOf(Props[JsonFetcher])

  def receive = {

    case IndexAllLunches => {
      println(s"Got IndexAllLunches")
      jsonFetcher ! IndexAllLunches
    }

    case Lunches(lunches) => {
      println(s"Indexer got ${lunches.size} lunches for indexing")
      val req: Solr.SolrOperation = Solr.Update AddDocs (lunches.map(_.toSolr):_*) commit true
      val resp: Future[SolrQueryResponse] =
        (Solr.Client.manager ? Solr.Request("http://localhost:8983/solr/obedy", req)).mapTo[SolrQueryResponse]
      resp.onComplete {
        case Success(value) => {
          log.debug(s"Solr returned ${value.status}")
          value.status match {
            case SolrException.ErrorCode.UNKNOWN.code ⇒ log.info(s"Succesfully added to SOLR: ${value}")
            case _ ⇒ log.error(s"Error adding to SOLR: ${value}")
          }
        }
        case Failure(NonFatal(e)) => log.error(s"Error communicating with SOLR: ${e}")
      }
    }
  }
}
