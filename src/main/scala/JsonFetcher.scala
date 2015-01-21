package akka_first
import akka.actor.{Actor, ActorLogging}
import akka_first.LunchProtocol.{Lunches, IndexAllLunches}
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
 * @author miso
 */
class JsonFetcher extends Actor with ActorLogging {
  import context.dispatcher

  def receive = {
    case IndexAllLunches => {
      println("JsonFetcher got request")
      val capturedSender = sender
      val url = "https://spreadsheets.google.com/feeds/list/0AmHPGqfSTzZhdE9qRzdIdU5qdS1RZkZINGx4aUl6a3c/od6/public/values?alt=json"
      val pipeline: HttpRequest => Future[Lunches] = sendReceive ~> unmarshal[Lunches]
      val response = pipeline(Get(url))
      response.onComplete {
        case Success(value) => {
          println("Got spreadsheet response")
          println(value)
          capturedSender ! value
        }
        case Failure(NonFatal(e)) => {
          println(s"Failure: ${e}")
        }
      }

    }
  }
}
