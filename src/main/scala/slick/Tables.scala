package slick

import scala.slick.driver.FreeOracleDriver.simple._
import scala.slick.util.TupleMethods._

/**
 * @author miso
 */

case class Request(id: Option[Int] = None, name: String, flag: Boolean)
class Requests(tag: Tag) extends Table[Request](tag, "INDEXING_REQUESTS") {
  def id: Column[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name: Column[String] = column[String]("name")
  def flag: Column[Boolean] = column[Boolean]("flag")
  def * = id.? ~ name ~ flag <> (Request.tupled, Request.unapply _)
  new String()
}

