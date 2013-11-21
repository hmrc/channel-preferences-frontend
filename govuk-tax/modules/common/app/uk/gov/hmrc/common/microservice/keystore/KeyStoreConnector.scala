package uk.gov.hmrc.common.microservice.keystore

import uk.gov.hmrc.microservice.{ MicroServiceConfig, Connector }
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import org.joda.time.DateTime
import controllers.common.actions.HeaderCarrier

case class KeyStore[T](id: String, dateCreated: DateTime, dateUpdated: DateTime, data: Map[String, T]) {
  def get(key: String): Option[T] = {
    data.get(key)
  }
}

class KeyStoreConnector(override val serviceUrl: String = MicroServiceConfig.keyStoreServiceUrl) extends Connector {

  def addKeyStoreEntry[T](id: String, source: String, key: String, data: T)(implicit manifest: Manifest[T], headerCarrier:HeaderCarrier) {
    val uri = buildUri(id, source) + s"/data/${key}"
    httpPut[KeyStore[T]](uri, Json.parse(toRequestBody(data)))
  }

  def getEntry[T](id: String, source: String, key: String)(implicit manifest: Manifest[T], hc: HeaderCarrier): Option[T] = {
    for {
      keyStore <- httpGet[KeyStore[T]](buildUri(id, source))
      value <- keyStore.get(key)
    } yield value
  }

  def getKeyStore[T](id: String, source: String)(implicit manifest: Manifest[T], hc: HeaderCarrier): Option[KeyStore[T]] = {
    httpGet[KeyStore[T]](buildUri(id, source))
  }

  def deleteKeyStore(id: String, source: String)(implicit hc: HeaderCarrier) {
    httpDeleteAndForget(buildUri(id, source))
  }

  def getDataKeys(id: String, source: String)(implicit hc: HeaderCarrier): Option[Set[String]] = {
    httpGet[Set[String]](buildUri(id, source) + "/data/keys")
  }

  private def buildUri(id: String, source: String) = s"/keystore/$source/$id"

}
