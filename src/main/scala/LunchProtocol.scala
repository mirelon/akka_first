package akka_first

import spray.http.HttpResponse
import spray.httpx.unmarshalling.{MalformedContent, FromResponseUnmarshaller}
import spray.json.lenses.JsonLenses._

/**
 * @author miso
 */
object LunchProtocol {
  import spray.json._

  def toSolrDate(date: String): String = {
    val slovakDate = """(\d+).(\d+).(\d\d\d\d)""".r
    val solrDate = """(\d\d\d\d-\d\d-\d\dT\d\d:\d\d:\d\dZ)""".r
    date match {
      case slovakDate(day, month, year) => f"${year}-${month.toInt}%02d-${day.toInt}%02dT00:00:00Z"
      case solrDate(value) => value
      case _ => "1900-01-01T00:00:00Z"
    }
  }

  case class Lunch(date:String, meal:String, restaurant:String) {
    def toSolr = Map("date" -> toSolrDate(date), "meal" -> meal, "restaurant" -> restaurant)

  }

  case class Lunches(lunches: Vector[Lunch])

  case object GetLunches

  case object IndexAllLunches

  //----------------------------------------------
  // JSON
  //----------------------------------------------

  object Lunch extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(Lunch.apply)

    def fromMap(doc: Map[String, AnyRef]) = {
      new Lunch(
        date = doc.get("date").orNull.toString,
        meal = doc.get("meal").orNull.toString,
        restaurant = doc.get("restaurant").orNull.toString
      )
    }
  }

  object Lunches extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(Lunches.apply)

    implicit val spreadsheetUnmarshaller = new FromResponseUnmarshaller[Lunches] {
      def apply(response: HttpResponse) = try {
        val rows = response.entity.asString.extract[JsObject]('feed / 'entry / *).drop(2).map(x => Lunch.fromMap(Map(
          "date" -> x.extract[String]('gsx$_cn6ca.? / '$t.?).getOrElse(""),
          "meal" -> x.extract[String]('gsx$_cokwr.? / '$t.?).getOrElse(""),
          "restaurant" -> x.extract[String]('gsx$_cpzh4.? / '$t.?).getOrElse("")
        )))
        Right(Lunches(rows.to[Vector]))
      } catch { case x: Throwable =>
        println(x)
        Left(MalformedContent("Could not unmarshall spreadsheet response.", x))
      }
    }

    def fromSpreadsheet(spreadsheet: String) = {
      new Lunches(Vector[Lunch]())
    }
  }

}
