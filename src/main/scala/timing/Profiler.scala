package timing

import akka.actor.{Actor, ActorLogging}
import timing.ProfilingProtocol.{Stop, Start}
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer

/**
 * @author miso
 */
class Profiler extends Actor with ActorLogging {
  var timers: Map[String, Long] = Map()
  var results: Map[String, ListBuffer[Long]] = Map()
  def receive = {
    case Start(what) => {
      log.debug(s"Started profiling ${what}")
      timers(what) = System.currentTimeMillis()
    }
    case Stop(what) => {
      if(timers.contains(what)) {
        val duration = System.currentTimeMillis() - timers(what)
        if(!results.contains(what)) {
          results(what) = new ListBuffer[Long]
        }
        results(what).append(duration)
        log.info(s"${what}: ${results(what).map(d => s"${d}ms").mkString(",")}")
      } else {
        log.error(s"${what} has not started")
      }
    }
  }
}
