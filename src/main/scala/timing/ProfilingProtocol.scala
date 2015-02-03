package timing

/**
 * @author miso
 */
object ProfilingProtocol {

  case class Start(what: String)

  case object Start

  case class Stop(what: String)

  case object Stop
}
