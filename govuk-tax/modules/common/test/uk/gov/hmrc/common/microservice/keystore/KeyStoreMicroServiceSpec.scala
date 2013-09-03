package uk.gov.hmrc.common.microservice.keystore

import play.api.test.{ FakeApplication, WithApplication }
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import org.mockito.{ ArgumentCaptor, Matchers, Mockito }
import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec

class TestKeyStoreMicroService extends KeyStoreMicroService with MockitoSugar {
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

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None
    def httpDeleteAndForget(uri: String) {}
  }
}

class KeyStoreMicroServiceSpec extends BaseSpec with MockitoSugar {

  "KeyStoreMicroService" should {
    "call the key store service when adding a key store entry " in new WithApplication(FakeApplication()) {

      val keyStoreMicroService = new TestKeyStoreMicroService()

      val id: String = "anId"
      val source: String = "aSource"
      val key: String = "aKey"
      val data = Map("key1" -> "value1", "key2" -> "value2")

      keyStoreMicroService.addKeyStoreEntry(id, source, key, data)

      private val captor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[JsValue]])

      verify(keyStoreMicroService.httpWrapper, times(1)).post[String](Matchers.eq("/keystore/aSource/anId/data/aKey"), captor.capture(), Matchers.any[Map[String, String]])

      val body = captor.getValue
      (body \ "key1").as[String] must be("value1")
      (body \ "key2").as[String] must be("value2")
    }

    "call the key store service when getting a key store " in new WithApplication(FakeApplication()) {

      val keyStoreMicroService = new TestKeyStoreMicroService()

      val id: String = "anId"
      val source: String = "aSource"

      keyStoreMicroService.getKeyStore[String](id, source)

      verify(keyStoreMicroService.httpWrapper, times(1)).get[String]("/keystore/aSource/anId")
    }

    case class SomeData(firstName: String, lastName: String)

    "retrieve a specific entry" in {

      val keyStoreMicroService = new TestKeyStoreMicroService()

      Mockito.when(keyStoreMicroService.httpWrapper.get[KeyStore[SomeData]]("/keystore/aSource/anId")).thenReturn(Some(KeyStore[SomeData]("anID", null, null, Map("key" -> Map("entryKey" -> SomeData("John", "Densmore"))))))

      val entry = keyStoreMicroService.getEntry[SomeData]("anId", "aSource", "key", "entryKey")

      verify(keyStoreMicroService.httpWrapper, times(1)).get[String]("/keystore/aSource/anId")
      entry must not be 'empty
      entry.get.firstName mustBe "John"
      entry.get.lastName mustBe "Densmore"

    }

    "handle existing keystore without an entry" in {

      val keyStoreMicroService = new TestKeyStoreMicroService()

      Mockito.when(keyStoreMicroService.httpWrapper.get[KeyStore[SomeData]]("/keystore/aSource/anId")).thenReturn(Some(KeyStore[SomeData]("anID", null, null, Map("key" -> Map("anotherEntry" -> SomeData("John", "Densmore"))))))

      val entry = keyStoreMicroService.getEntry[SomeData]("anId", "aSource", "key", "entryKey")

      entry mustBe None

    }

    "call the key store service when deleting a key store " in new WithApplication(FakeApplication()) {

      val keyStoreMicroService = new TestKeyStoreMicroService()

      val id: String = "anId"
      val source: String = "aSource"

      keyStoreMicroService.deleteKeyStore(id, source)

      verify(keyStoreMicroService.httpWrapper, times(1)).httpDeleteAndForget("/keystore/aSource/anId")
    }

    "call the key store service when getting the data keys" in new WithApplication(FakeApplication()) {

      val keyStoreMicroService = new TestKeyStoreMicroService()

      val id: String = "anId"
      val source: String = "aSource"

      keyStoreMicroService.getDataKeys(id, source)

      verify(keyStoreMicroService.httpWrapper, times(1)).get[String]("/keystore/aSource/anId/data/keys")

    }

  }
}
