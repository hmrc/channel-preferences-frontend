package uk.gov.hmrc.common.microservice.keystore

import play.api.test.{ FakeApplication, WithApplication }
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import org.mockito.{ ArgumentCaptor, Matchers, Mockito }
import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec

class TestKeyStoreConnector extends KeyStoreConnector with MockitoSugar {
  val httpWrapper = mock[HttpWrapper]

  override protected def httpPutAndForget(uri: String, body: JsValue, headers: Map[String, String] = Map.empty) {
    httpWrapper.post(uri, body, headers)
  }

  override protected def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.get[A](uri)
  }

  override protected def httpDeleteAndForget(uri: String) {
    httpWrapper.httpDeleteAndForget(uri)
  }

  override protected def httpPut[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty)(implicit m: Manifest[A]): Option[A] = {
    httpWrapper.httpPut[A](uri, body)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None
    def httpDeleteAndForget(uri: String) {}
    def httpPut[A](uri: String, body: JsValue, headers: Map[String, String] = Map.empty): Option[A] = None
  }
}

class KeyStoreConnectorSpec extends BaseSpec with MockitoSugar {

  "KeyStoreConnector" should {
    "call the key store service when adding a key store entry " in new WithApplication(FakeApplication()) {

      val keyStoreConnector = new TestKeyStoreConnector()

      val id: String = "anId"
      val source: String = "aSource"
      val key: String = "aKey"
      val data = Map("key1" -> "value1", "key2" -> "value2")

      keyStoreConnector.addKeyStoreEntry(id, source, key, data)

      private val captor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])

      verify(keyStoreConnector.httpWrapper, times(1)).httpPut[String](Matchers.eq("/keystore/aSource/anId/data/aKey"), captor.capture(), Matchers.any[Map[String, String]])

      val body = captor.getValue
      (body \ "key1").as[String] should be("value1")
      (body \ "key2").as[String] should be("value2")
    }

    "call the key store service when getting a key store " in new WithApplication(FakeApplication()) {

      val keyStoreConnector = new TestKeyStoreConnector()

      val id: String = "anId"
      val source: String = "aSource"

      keyStoreConnector.getKeyStore[String](id, source)

      verify(keyStoreConnector.httpWrapper, times(1)).get[String]("/keystore/aSource/anId")
    }

    case class SomeData(firstName: String, lastName: String)

    "retrieve a specific entry" in {

      val keyStoreConnector = new TestKeyStoreConnector()

      Mockito.when(keyStoreConnector.httpWrapper.get[KeyStore[SomeData]]("/keystore/aSource/anId")).thenReturn(Some(KeyStore[SomeData]("anID", null, null, Map("entryKey" -> SomeData("John", "Densmore")))))

      val entry = keyStoreConnector.getEntry[SomeData]("anId", "aSource", "entryKey")

      verify(keyStoreConnector.httpWrapper, times(1)).get[String]("/keystore/aSource/anId")
      entry should not be 'empty
      entry.get.firstName shouldBe "John"
      entry.get.lastName shouldBe "Densmore"

    }

    "handle existing keystore without an entry" in {

      val keyStoreConnector = new TestKeyStoreConnector()

      Mockito.when(keyStoreConnector.httpWrapper.get[KeyStore[SomeData]]("/keystore/aSource/anId")).thenReturn(Some(KeyStore[SomeData]("anID", null, null, Map("anotherEntry" -> SomeData("John", "Densmore")))))

      val entry = keyStoreConnector.getEntry[SomeData]("anId", "aSource", "entryKey")

      entry shouldBe None

    }

    "call the key store service when deleting a key store " in new WithApplication(FakeApplication()) {

      val keyStoreConnector = new TestKeyStoreConnector()

      val id: String = "anId"
      val source: String = "aSource"

      keyStoreConnector.deleteKeyStore(id, source)

      verify(keyStoreConnector.httpWrapper, times(1)).httpDeleteAndForget("/keystore/aSource/anId")
    }

    "call the key store service when getting the data keys" in new WithApplication(FakeApplication()) {

      val keyStoreConnector = new TestKeyStoreConnector()

      val id: String = "anId"
      val source: String = "aSource"

      keyStoreConnector.getDataKeys(id, source)

      verify(keyStoreConnector.httpWrapper, times(1)).get[String]("/keystore/aSource/anId/data/keys")

    }

  }
}
