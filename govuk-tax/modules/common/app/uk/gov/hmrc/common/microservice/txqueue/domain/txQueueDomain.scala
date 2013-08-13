package microservice.txqueue

import java.net.{ URL, URI }
import org.joda.time.DateTime

/**
 * @author Eric Vlaanderen
 */
object TxQueueRegime {

}
case class TxQueueTransaction(id: URI, regime: String, user: URI, callback: Option[Callback], statusHistory: List[Status],
  tags: Option[List[String]] = None, createdAt: DateTime, lastUpdatedAt: DateTime)

case class Callback(regimeService: URL, body: String, headers: Map[String, String] = Map.empty, httpMethod: String)
case class Status(status: String, message: Option[String] = None, createdAt: DateTime)