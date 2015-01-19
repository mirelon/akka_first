import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import com.codemettle.akkasolr.Solr
import com.codemettle.akkasolr.solrtypes.SolrQueryResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

import ExecutionContext.Implicits.global

/**
 * @author miso
 */
class Actress extends Actor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)
  override def receive: Receive = {
    case Raven =>
      log.info("But I have ordered a crow!")
      val doc = Map("date" -> "2015-01-16T00:00:00Z", "meal" -> "Pecena vrana", "restaurant" -> "Club Restaurant")
      val req: Solr.SolrOperation = Solr.Update AddDocs doc commit true
      val resp: Future[SolrQueryResponse] =
        (Solr.Client.manager ? Solr.Request("http://localhost:8983/solr/obedy", req)).mapTo[SolrQueryResponse]
      resp.onComplete {
        case Success(value) => println(value)
        case Failure(NonFatal(e)) => println(e)
      }
      log.info("Message to SOLR sent")
  }
}
