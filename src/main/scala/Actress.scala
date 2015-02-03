package akka_first
import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout

import scala.concurrent.duration._

/**
 * @author miso
 */
class Actress extends Actor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)
  override def receive: Receive = {
    case Raven =>
      log.info("But I have ordered a crow!")
  }
}
