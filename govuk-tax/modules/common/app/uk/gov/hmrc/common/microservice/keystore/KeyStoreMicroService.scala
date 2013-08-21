package uk.gov.hmrc.common.microservice.keystore

import uk.gov.hmrc.microservice.{ MicroServiceConfig, MicroService }
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import org.joda.time.DateTime

case class KeyStore(id: String, dateCreated: DateTime, dateUpdated: DateTime, data: Map[String, Any])

class KeyStoreMicroService(override val serviceUrl: String = MicroServiceConfig.keyStoreServiceUrl) extends MicroService {

  def addKeyStoreEntry(id: String, source: String, key: String, data: Map[String, Any]){
    val uri = buildUri(id, source)  + s"/data/${key}"
    httpPutAndForget(uri, Json.parse(toRequestBody(data)))
  }

  def getKeyStore(id: String, source: String): KeyStore = {
    httpGet[KeyStore](buildUri(id, source)).getOrElse(null)
  }

  def deleteKeyStore(id: String, source: String) {
    httpDeleteAndForget(buildUri(id, source))
  }

  def buildUri(id: String, source: String) = s"/keystore/${source}/${id}"

}
