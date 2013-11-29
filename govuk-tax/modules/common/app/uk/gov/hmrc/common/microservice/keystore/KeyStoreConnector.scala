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

  def addKeyStoreEntry[T](actionId: String, source: String, formId: String, data: T, ignoreSession: Boolean = false)(implicit manifest: Manifest[T], headerCarrier:HeaderCarrier) {
    val keyStoreId = generateKeyStoreId(actionId, ignoreSession)
    val uri = buildUri(keyStoreId, source) + s"/data/$formId"
    httpPut[KeyStore[T]](uri, Json.parse(toRequestBody(data)))
  }

  def getEntry[T](actionId: String, source: String, formId: String, ignoreSession: Boolean = false)(implicit manifest: Manifest[T], hc: HeaderCarrier): Option[T] = {
    val keyStoreId = generateKeyStoreId(actionId, ignoreSession)
    for {
      keyStore <- httpGet[KeyStore[T]](buildUri(keyStoreId, source))
      value <- keyStore.get(formId)
    } yield value
  }

  def getKeyStore[T](actionId: String, source: String, ignoreSession: Boolean = false)(implicit manifest: Manifest[T], hc: HeaderCarrier): Option[KeyStore[T]] = {
    val keyStoreId = generateKeyStoreId(actionId, ignoreSession)
    httpGet[KeyStore[T]](buildUri(keyStoreId, source))
  }

  def deleteKeyStore(actionId: String, source: String, ignoreSession: Boolean = false)(implicit hc: HeaderCarrier) {
    val keyStoreId = generateKeyStoreId(actionId, ignoreSession)
    httpDeleteAndForget(buildUri(keyStoreId, source))
  }

  def getDataKeys(actionId: String, source: String, ignoreSession: Boolean = false)(implicit hc: HeaderCarrier): Option[Set[String]] = {
    val keyStoreId = generateKeyStoreId(actionId, ignoreSession)
    httpGet[Set[String]](buildUri(keyStoreId, source) + "/data/keys")
  }

  private def buildUri(id: String, source: String) = s"/keystore/$source/$id"

  private def generateKeyStoreId(actionId: String, ignoreSession: Boolean)(implicit headerCarrier: HeaderCarrier) = {
    val userId = headerCarrier.userId.map{userId => userId.substring(userId.lastIndexOf("/") + 1)}.getOrElse("unknownUserId")
    val sessionId = generateSessionIdForKeyStoreId(ignoreSession)
    s"$userId:$actionId$sessionId"
  }

  private def generateSessionIdForKeyStoreId(ignoreSession: Boolean)(implicit headerCarrier: HeaderCarrier) = {
    if(ignoreSession == false) {
      val sessionId = headerCarrier.sessionId.getOrElse("unknownSessionId")
      s":$sessionId"
    }
    else ""
  }

}
