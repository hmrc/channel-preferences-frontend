package uk.gov.hmrc.common.microservice.txqueue.domain

import java.net.{ URL, URI }
import org.joda.time.DateTime

object TxQueueRegime {

}
case class TxQueueTransaction(id: URI, regime: String, user: URI, callback: Option[Callback], statusHistory: List[Status],
  tags: Option[List[String]] = None, properties: Map[String, String], createdAt: DateTime, lastUpdatedAt: DateTime)

case class Callback(regimeService: URL, body: String, headers: Map[String, String] = Map.empty, httpMethod: String)
case class Status(status: String, message: Option[String] = None, createdAt: DateTime)