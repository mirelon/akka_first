package akka_first
import akka.actor.{Actor, ActorLogging}
import akka_first.LunchProtocol.Lunch

/**
 * @author miso
 */
class RestaurantProcessor extends Actor with ActorLogging {
  def receive = {
    case x: Lunch => {
      sender ! new Lunch(meal = x.meal, restaurant = "prefix_" + x.restaurant, date = x.date)
    }
    case _ => ???
  }
}
