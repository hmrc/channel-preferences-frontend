package uk.gov.hmrc.common.microservice.keystore

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import org.joda.time.DateTime

case class KeyStore[T](id: String, dateCreated: DateTime, dateUpdated: DateTime, data: Map[String, T]) {
  def get(key: String): Option[T] = {
    data.get(key)
  }
}

class KeyStoreMicroService(override val serviceUrl: String = MicroServiceConfig.keyStoreServiceUrl) extends MicroService {

  def addKeyStoreEntry[T](id: String, source: String, key: String, data: T)(implicit manifest: Manifest[T]) {
    val uri = buildUri(id, source) + s"/data/${key}"
    httpPut[KeyStore[T]](uri, Json.parse(toRequestBody(data)))
  }

  def getEntry[T](id: String, source: String, key: String)(implicit manifest: Manifest[T]): Option[T] = {
    for {
      keyStore <- httpGet[KeyStore[T]](buildUri(id, source))
      value <- keyStore.get(key)
    } yield value
  }

  def getKeyStore[T](id: String, source: String)(implicit manifest: Manifest[T]): Option[KeyStore[T]] = {
    httpGet[KeyStore[T]](buildUri(id, source))
  }

  def deleteKeyStore(id: String, source: String) {
    httpDeleteAndForget(buildUri(id, source))
  }

  def getDataKeys(id: String, source: String): Option[Set[String]] = {
    httpGet[Set[String]](buildUri(id, source) + "/data/keys")
  }

  private def buildUri(id: String, source: String) = s"/keystore/$source/$id"

}
