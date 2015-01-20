package akka_first

import akka.actor.{ActorLogging, PoisonPill, Actor}
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

object LunchProtocol {
  import spray.json._

  case class Lunch(date:String, meal:String, restaurant:String) {
    def toSolr = Map("date" -> date, "meal" -> meal, "restaurant" -> restaurant)

  }

  case class Lunches(lunches: Vector[Lunch])

  case object GetLunches

  //----------------------------------------------
  // JSON
  //----------------------------------------------

  object Lunch extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Lunch.apply)

    def fromSolr(doc: Map[String, AnyRef]) = {
      new Lunch(
        date = doc.get("date").orNull.toString,
        meal = doc.get("meal").orNull.toString,
        restaurant = doc.get("restaurant").orNull.toString
      )
    }
  }

  object Lunches extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(Lunches.apply)
  }

}

class Indexer extends Actor with ActorLogging {
  import LunchProtocol._
  import spray.json._
  import context.dispatcher

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
          log.debug(s"Results: ${value.results.documents.map(_.asMap).map(doc => Lunch.fromSolr(doc))}")
          value.status match {
            case SolrException.ErrorCode.UNKNOWN.code ⇒ {
              log.info(s"Succesfully selected from SOLR: ${value}")
              val lunches = Lunches(value.results.documents.map(_.asMap).map(doc => Lunch.fromSolr(doc)).to[Vector])
              capturedSender ! lunches
            }
            case _ ⇒ log.error(s"Error selecting from SOLR: ${value}")
          }
        }
        case Failure(NonFatal(e)) => log.error(s"Error communicating with SOLR: ${e}")
      }
    }

    case Lunches(lunches) => {
      println(s"Got lunches: ${lunches.toJson}")
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
