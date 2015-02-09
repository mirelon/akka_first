package slick

import scala.slick.driver.FreeSQLServerDriver.simple._
import scala.slick.util.TupleMethods._

/**
 * @author miso
 */

case class Request(id: Option[Int] = None, name: String)
class Requests(tag: Tag) extends Table[Request](tag, "requests") {
  def id: Column[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name: Column[String] = column[String]("name")
  def * = id.? ~ name <> (Request.tupled, Request.unapply _)
}
