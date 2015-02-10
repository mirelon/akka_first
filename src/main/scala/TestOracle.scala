package akka_first

import slick.{Request, Requests}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.driver.FreeOracleDriver.simple._
import scala.slick.driver.MySQLDriver
import Database.dynamicSession
import scala.slick.lifted.TableQuery

/**
 * Created by miso on 10.2.2015.
 */
object TestOracle extends App {
  val jdbcUrl = "jdbc:oracle:thin:@localhost:11521:SVPTU01"
  val db = Database.forURL(jdbcUrl, driver = "oracle.jdbc.OracleDriver", user="system", password=sys.env("ORACLE_DB_PASS"))
  val requests = TableQuery[Requests]
  db.withDynSession {
    println("adding row")
    requests += Request(None, "name42", false)
    println(requests.filter(_.flag).run.toString)
  }
}
