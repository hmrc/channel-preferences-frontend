package uk.gov.hmrc.common.microservice.keystore

import play.api.test.{FakeApplication, WithApplication}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.mockito.Mockito._
import uk.gov.hmrc.common.BaseSpec
import controllers.common.actions.HeaderCarrier
import org.specs2.specification.BeforeEach
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future

class TestKeyStoreConnector extends KeyStoreConnector with MockitoSugar {
  val httpWrapper = mock[HttpWrapper]

  override protected def httpGetF[A](uri: String)(implicit m: Manifest[A], hc: HeaderCarrier): Future[Option[A]] = {
    httpWrapper.getF[A](uri)
  }

  override protected def httpDeleteAndForget(uri: String)(implicit hc: HeaderCarrier) {
    httpWrapper.httpDeleteAndForget(uri)
  }

  override protected def httpPutF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] = {
    httpWrapper.httpPutF[A, B](uri, body)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None

    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)

    def httpDeleteAndForget(uri: String) {}

    def httpPutF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty): Future[Option[B]] = Future.successful(None)
  }

}

class KeyStoreConnectorSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  val actionId = "anActionId"
  val source: String = "aSource"
  val formId: String = "aFormId"
  val data = Map("key1" -> "value1", "key2" -> "value2")

  val userId = "1234567890"
  val sessionId = "378ej373y3g3t3t63g3ehd7337329049j"
  val keyStoreId = s"$userId:$actionId:$sessionId"

  override implicit val hc: HeaderCarrier = HeaderCarrier(Some(userId), sessionId = Some(sessionId))

  "KeyStoreConnector" should {

    "call the key store service when adding a key store entry " in new WithApplication(FakeApplication()) {

      val headerCarrier = HeaderCarrier(Some(userId), sessionId = Some(sessionId))

      val keyStoreConnector = new TestKeyStoreConnector()
      keyStoreConnector.addKeyStoreEntry(actionId, source, formId, data)

      private val captor: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(manifest.runtimeClass.asInstanceOf[Class[Map[String, String]]])
      verify(keyStoreConnector.httpWrapper, times(1)).httpPutF[Map[String, String], KeyStore[Map[String, String]]](Matchers.eq(s"/keystore/$source/$keyStoreId/data/$formId"), captor.capture(), Matchers.any[Map[String, String]])

      val body = captor.getValue
      body("key1") shouldBe "value1"
      body("key2") shouldBe "value2"
    }

    "call the key store service when getting a key store " in new WithApplication(FakeApplication()) {

      val keyStoreConnector = new TestKeyStoreConnector()

      when(keyStoreConnector.httpWrapper.getF[KeyStore[SomeData]](s"/keystore/$source/$keyStoreId")).thenReturn(None)

      val f = keyStoreConnector.getKeyStore[String](actionId, source)
      whenReady(f) {
        keys =>
          verify(keyStoreConnector.httpWrapper, times(1)).getF[String](s"/keystore/aSource/$keyStoreId")
      }
    }

    case class SomeData(firstName: String, lastName: String)

    "retrieve a specific entry" in {

      val keyStoreConnector = new TestKeyStoreConnector()

      Mockito.when(keyStoreConnector.httpWrapper.getF[KeyStore[SomeData]](s"/keystore/$source/$keyStoreId")).thenReturn(Some(KeyStore[SomeData](keyStoreId, null, null, Map("entryKey" -> SomeData("John", "Densmore")))))

      whenReady(keyStoreConnector.getEntry[SomeData](actionId, source, "entryKey")) {
        entry =>
          verify(keyStoreConnector.httpWrapper, times(1)).getF[String](s"/keystore/$source/$keyStoreId")
          entry should not be 'empty
          entry.get.firstName shouldBe "John"
          entry.get.lastName shouldBe "Densmore"
      }
    }

    "handle existing keystore without an entry" in {

      val keyStoreConnector = new TestKeyStoreConnector()

      Mockito.when(keyStoreConnector.httpWrapper.getF[KeyStore[SomeData]](s"/keystore/$source/$keyStoreId")).thenReturn(Some(KeyStore[SomeData](keyStoreId, null, null, Map("anotherEntry" -> SomeData("John", "Densmore")))))

      whenReady(keyStoreConnector.getEntry[SomeData](actionId, source, "entryKey")) {
        entry =>
          entry shouldBe None
      }

    }

    "call the key store service when deleting a key store " in new WithApplication(FakeApplication()) {

      val keyStoreConnector = new TestKeyStoreConnector()

      keyStoreConnector.deleteKeyStore(actionId, source)

      verify(keyStoreConnector.httpWrapper, times(1)).httpDeleteAndForget(s"/keystore/$source/$keyStoreId")
    }
  }
}
