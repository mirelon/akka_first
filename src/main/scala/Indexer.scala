package akka_first

import akka.actor.{PoisonPill, Actor}

object LunchProtocol {
  import spray.json._

  case class Lunch(event:String, nrOfTickets:Int)

  case class Lunches(lunches: Vector[Lunch])

  case object GetLunches

  //----------------------------------------------
  // JSON
  //----------------------------------------------

  object Lunch extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Lunch.apply)
  }

  object Lunches extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(Lunches.apply)
  }

}

class Indexer extends Actor {
  import LunchProtocol._
  import spray.json._

  def receive = {

    case GetLunches => {
      val lunch = Lunch(event="E", nrOfTickets = 10)
      val lunches = Lunches(Vector[Lunch](lunch))
      println(s"JSON: ${lunches.toJson}")
      sender ! lunches
    }
  }
}
