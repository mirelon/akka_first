package akka_first
import akka.actor.{Actor, ActorLogging}
import akka_first.LunchProtocol.{Lunches, IndexAllLunches}
import spray.client.pipelining._
import spray.http.HttpRequest

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author miso
 */
class JsonFetcher extends Actor with ActorLogging {

  def receive = {
    case IndexAllLunches => {
      val capturedSender = sender
      val url = "https://spreadsheets.google.com/feeds/list/0AmHPGqfSTzZhdE9qRzdIdU5qdS1RZkZINGx4aUl6a3c/od6/public/values?alt=json"
      val pipeline: HttpRequest => Future[Lunches] = sendReceive ~> unmarshal[Lunches]
      val response = pipeline(Get(url))
      log.info(s"Sent request to ${url}")
      response.onComplete {
        case Success(value) => {
          log.debug("Got spreadsheet response")
          capturedSender ! value
        }
        case Failure(NonFatal(e)) => {
          log.error(s"Failure when communicating with spreadsheet API: ${e}")
        }
      }

    }
  }
}
