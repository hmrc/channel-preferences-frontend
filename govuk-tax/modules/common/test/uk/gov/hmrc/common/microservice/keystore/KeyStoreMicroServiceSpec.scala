package uk.gov.hmrc.common.microservice.keystore

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.audit.{AuditMicroService, AuditEvent}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import org.joda.time.DateTime

class TestKeyStoreMicroService extends KeyStoreMicroService {
  var body: JsValue = null
  var uri: String = null

  override protected def httpPutAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) {
    this.body = body
    this.uri = uri
  }

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    this.uri = uri
    None
  }

  override protected def httpDeleteAndForget(uri: String) {
    this.uri = uri
    None
  }
}

class KeyStoreMicroServiceSpec extends WordSpec with MustMatchers with MockitoSugar {

    "KeyStoreMicroService" should {
      "call the key store service when adding a key store entry " in new WithApplication(FakeApplication()) {

        val keyStoreMicroService = new TestKeyStoreMicroService()

        val id: String = "anId"
        val source: String = "aSource"
        val key: String = "aKey"
        val data = Map("key1" -> "value1", "key2" -> "value2")

        keyStoreMicroService.addKeyStoreEntry(id, source, key, data)

        keyStoreMicroService.uri must be("/keystore/aSource/anId/data/aKey")

        val body = keyStoreMicroService.body
        (body \ "key1").as[String] must be("value1")
        (body \ "key2").as[String] must be("value2")
      }

      "call the key store service when getting a key store " in new WithApplication(FakeApplication()) {

        val keyStoreMicroService = new TestKeyStoreMicroService()

        val id: String = "anId"
        val source: String = "aSource"

        keyStoreMicroService.getKeyStore(id, source)

        keyStoreMicroService.uri must be("/keystore/aSource/anId")
      }

      "call the key store service when deleting a key store " in new WithApplication(FakeApplication()) {

        val keyStoreMicroService = new TestKeyStoreMicroService()

        val id: String = "anId"
        val source: String = "aSource"

        keyStoreMicroService.deleteKeyStore(id, source)

        keyStoreMicroService.uri must be("/keystore/aSource/anId")
      }
    }
}
