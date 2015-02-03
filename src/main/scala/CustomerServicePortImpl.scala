package cc.notsoclever.customerservice

import java.util

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import akka_first.{Searcher}
import akka_first.LunchProtocol._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConversions._

/**
 * @author miso
 */
class CustomerServicePortImpl(system: ActorSystem) extends CustomerService {

  def this() {this(null)} // needed due to generated code in CustomerService_CustomerServicePort_Server

  implicit val timeout = Timeout(100 seconds)
  val indexer = system.actorSelection("akka.tcp://akka-first-actor-system@localhost:5002/user/indexer")
  val searcher = system.actorOf(Props[Searcher], "searcher")

  override def getCustomersByName(name: String): util.List[Customer] = {

    val future = searcher.ask(GetLunches)
    println("Message asked, awaiting result")
    val result = Await.result(future, timeout.duration)
    println("Result is ready")
    println(result)
    Await.result(future, timeout.duration) match {
      case result: Lunches => {
        val customer = new Customer()
        customer.setCustomerId(result.lunches.size)
        Seq(customer)
      }
      case _ => null
    }
  }

  override def updateCustomer(customer: Customer): Customer = ???
}
